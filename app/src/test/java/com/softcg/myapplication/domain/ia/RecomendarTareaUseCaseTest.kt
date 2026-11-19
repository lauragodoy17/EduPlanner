package com.softcg.myapplication.domain.ia

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.data.network.RespuestaApi
import com.softcg.myapplication.domain.prediccion.DatosPrediccion
import com.softcg.myapplication.domain.prediccion.EstadoDato
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ExtraerDatosPrediccionUseCase
import com.softcg.myapplication.domain.prediccion.ItemDato
import com.softcg.myapplication.domain.prediccion.PredecirTareaUseCase
import com.softcg.myapplication.domain.prediccion.ResultadoExtraccion
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime

class RecomendarTareaUseCaseTest {

    private val reloj = RelojFijo(LocalDateTime.of(2026, 11, 12, 10, 0))
    private val tarea = Tarea(1, "Integrales por partes", "Ejercicios 1 a 10", "Cálculo", "2026-11-20", 1, asignaturaId = 3)
    private val asignatura = Asignatura(3, "Cálculo", "Ana", 2, "8:00", List(7) { it == 0 }, areaModelo = "Matematicas")

    private val datos = DatosPrediccion(8.0, 2.5, 7.75, 5.0, 7.5, "Matematicas", "Integrales", "Pomodoro")

    /** Respuesta real de /predict (forma de api.md). */
    private val respuestaApi = """
        {"prediccion": {"attendance_avg": 8.0, "intensity_hours": 2.5, "grade_avg": 7.75, "difficulty_perception": 5.0,
          "interest_level": 7.5, "subject_name": "Matematicas", "subject_topic": "Integrales", "study_method": "Pomodoro",
          "practice_label": "Reforzar", "practice_confianza": 0.61,
          "prob_Debe practicar": 0.12, "prob_Dominada": 0.27, "prob_Reforzar": 0.61,
          "metodo_top1": "Mapas mentales", "metodo_top1_prob": 0.3412,
          "metodo_top2": "Pomodoro", "metodo_top2_prob": 0.21,
          "metodo_top3": "Fichas de estudio", "metodo_top3_prob": 0.1,
          "cluster": 2, "pca_1": -0.53, "pca_2": 1.08}, "avisos": []}
    """.trimIndent()

    private val extraer: ExtraerDatosPrediccionUseCase = mockk()
    private val predecir: PredecirTareaUseCase = mockk()

    /** Responde en orden; la última respuesta se repite. */
    private class GeneradorFalso(vararg respuestas: () -> String) : GeneradorRecomendaciones {
        private val pendientes = respuestas.toMutableList()
        var llamadas = 0
        val prompts = mutableListOf<String>()
        override suspend fun generar(prompt: String): String {
            llamadas++
            prompts += prompt
            return (if (pendientes.size > 1) pendientes.removeAt(0) else pendientes.first())()
        }
    }

    private class VerificadorFalso(val aceptadas: Set<String>) : VerificadorFuentes {
        var recibidas = emptyList<FuentePropuesta>()
        override suspend fun verificar(propuestas: List<FuentePropuesta>): List<Fuente> {
            recibidas = propuestas
            return propuestas.filter { it.titulo in aceptadas }.map { Fuente(it.tipo, it.titulo, it.autor, "https://ejemplo.org/${it.titulo.length}") }
        }
    }

    private fun gemini(metodo: String, intro: String) = """
        {"metodo":"$metodo","intro":"$intro","consejo":"Pon la fórmula de integración por partes en el centro.",
         "comoAplicar":["Escribe la fórmula"," Ramifica los casos ",""],"tiempoEstimado":"2 horas",
         "fuentes":[{"tipo":"libro","titulo":"Cálculo de una variable","autor":"James Stewart","url":""},
                    {"tipo":"web","titulo":"Inventada","autor":"Nadie","url":"https://no.existe/x"},
                    {"tipo":"otro","titulo":"Sin tipo","autor":"","url":""}]}
    """.trimIndent()

    private val introConModelo = "Según los datos que has registrado en EduPlanner, te recomiendo Mapas mentales con un 34 % de confianza."
    private val respuestaConModelo = gemini("Mapas mentales", introConModelo)

    private fun caso(generador: GeneradorRecomendaciones, verificador: VerificadorFuentes = VerificadorFalso(setOf("Cálculo de una variable"))) =
        RecomendarTareaUseCase(generador, reloj, extraer, predecir, verificador)

    private fun extraccionCompleta() = Extraccion.Lista(
        ResultadoExtraccion(asignatura, tarea, listOf(EstadoDato(ItemDato.AREA, true, "Matemáticas", "")), datos, emptyList())
    )

    @Before
    fun setUp() {
        coEvery { extraer.deTarea(any()) } returns extraccionCompleta()
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(200, respuestaApi)
    }

    // INTEGRACIÓN CON EL MODELO PREDICTIVO

    @Test
    fun `consulta el modelo y recomienda el metodo con la confianza mas alta`() = runTest {
        val generador = GeneradorFalso({ respuestaConModelo })

        val r = (caso(generador)(tarea) as ResultadoRecomendacion.Exito).recomendacion

        coVerify { predecir(datos, any()) }
        assertEquals("Mapas mentales", r.metodo)
        assertEquals("34 %", r.confianza)
        assertEquals(introConModelo, r.intro)
        assertEquals(listOf("Escribe la fórmula", "Ramifica los casos"), r.comoAplicar)
        assertTrue(r.contexto is ContextoPrediccion.ConPrediccion)
    }

    @Test
    fun `la prediccion va dentro del prompt de Gemini`() = runTest {
        val generador = GeneradorFalso({ respuestaConModelo })
        caso(generador)(tarea)

        val prompt = generador.prompts.single()
        assertTrue(prompt.contains("Método de estudio recomendado: Mapas mentales, con 34 % de confianza"))
        assertTrue(prompt.contains("exactamente \"Mapas mentales\""))
        assertTrue(prompt.contains("Pomodoro (21 %)"))
        assertTrue(prompt.contains("Estado del estudiante en el tema: Reforzar (61 % de confianza)"))
        assertTrue(prompt.contains("Asistencia a clase: 80 %"))
        // El tema de la tarea, y de la bibliografía, es su título
        assertTrue(prompt.contains("Tema (es el título de la tarea): Integrales por partes"))
        assertTrue(prompt.contains("profundizar en el tema \"Integrales por partes\""))
        // Sin líneas con la sangría del código fuente
        assertFalse(prompt.lines().any { it.startsWith("        ") })
    }

    @Test
    fun `si Gemini cambia el metodo o no cita la confianza se descarta y se pide otra vez`() = runTest {
        val cambiaMetodo = gemini("Pomodoro", "Según tus datos te recomiendo Pomodoro con un 34 % de confianza.")
        val sinConfianza = gemini("Mapas mentales", "Según tus datos te recomiendo Mapas mentales.")

        val reintento = GeneradorFalso({ cambiaMetodo }, { respuestaConModelo })
        assertTrue(caso(reintento)(tarea) is ResultadoRecomendacion.Exito)
        assertEquals(2, reintento.llamadas)

        val siempreMal = GeneradorFalso({ sinConfianza })
        assertEquals(ResultadoRecomendacion.Error, caso(siempreMal)(tarea))
        assertEquals(2, siempreMal.llamadas)
    }

    @Test
    fun `la introduccion no puede recomendar otro metodo, salvo nombrar el que el estudiante ya usa`() {
        val caso = caso(GeneradorFalso({ "" }))
        val contexto = runBlockingContexto(caso)
        val otro = caso.interpretar(gemini("Mapas mentales", "Te recomiendo Mapas mentales con un 34 % de confianza, o Repaso espaciado."))!!
        val habitual = caso.interpretar(gemini("Mapas mentales", "Aunque sueles usar Pomodoro, te recomiendo Mapas mentales con un 34 % de confianza."))!!

        assertTrue(caso.validar(otro, contexto) != null)
        assertNull(caso.validar(habitual, contexto))
    }

    private fun runBlockingContexto(caso: RecomendarTareaUseCase) =
        kotlinx.coroutines.runBlocking { caso.contextoDe(tarea) }

    // SIN DATOS SUFICIENTES

    @Test
    fun `sin datos suficientes no consulta el modelo y lo dice abiertamente`() = runTest {
        coEvery { extraer.deTarea(any()) } returns Extraccion.Lista(
            ResultadoExtraccion(
                asignatura, tarea,
                listOf(
                    EstadoDato(ItemDato.NOTAS, true, "7,8 / 10", ""),
                    EstadoDato(ItemDato.ESTUDIO, false, "", "Falta con qué método estudias esta asignatura")
                ),
                datos = null, avisos = emptyList()
            )
        )
        val intro = "No tengo suficiente información sobre ti, sin embargo te recomiendo Fichas de estudio para esta tarea."
        val generador = GeneradorFalso({ gemini("Fichas de estudio", intro) })

        val r = (caso(generador)(tarea) as ResultadoRecomendacion.Exito).recomendacion

        coVerify(exactly = 0) { predecir(any(), any()) }
        assertEquals("Fichas de estudio", r.metodo)
        assertNull(r.confianza)
        val contexto = r.contexto as ContextoPrediccion.SinPrediccion
        assertEquals(MotivoSinPrediccion.FALTAN_DATOS, contexto.motivo)
        assertEquals(listOf("Tiempo y método de estudio"), contexto.faltantes)
        val prompt = generador.prompts.single()
        assertTrue(prompt.contains("empiece exactamente con \"No tengo suficiente información sobre ti, sin embargo te recomiendo\""))
        assertTrue(prompt.contains("faltan: Tiempo y método de estudio"))
    }

    @Test
    fun `sin prediccion la respuesta debe empezar aclarandolo y no inventar porcentajes`() {
        val caso = caso(GeneradorFalso({ "" }))
        val contexto = ContextoPrediccion.SinPrediccion(MotivoSinPrediccion.FALTAN_DATOS, emptyList())
        val bien = caso.interpretar(gemini("Pomodoro", "No tengo suficiente información sobre ti, sin embargo te recomiendo Pomodoro."))!!
        val sinAclarar = caso.interpretar(gemini("Pomodoro", "Te recomiendo Pomodoro."))!!
        val conPorcentaje = caso.interpretar(gemini("Pomodoro", "No tengo suficiente información sobre ti, sin embargo te recomiendo Pomodoro (90 %)."))!!

        assertNull(caso.validar(bien, contexto))
        assertTrue(caso.validar(sinAclarar, contexto) != null)
        assertTrue(caso.validar(conPorcentaje, contexto) != null)
    }

    @Test
    fun `una tarea sin asignatura registrada tampoco tiene prediccion`() = runTest {
        coEvery { extraer.deTarea(any()) } returns Extraccion.SinAsignatura
        val contexto = caso(GeneradorFalso({ "" })).contextoDe(tarea) as ContextoPrediccion.SinPrediccion
        assertEquals(MotivoSinPrediccion.FALTAN_DATOS, contexto.motivo)
    }

    @Test
    fun `si la API falla se usa la otra apertura`() = runTest {
        coEvery { predecir(any(), any()) } returns RespuestaApi.ErrorRed("timeout")
        val caso = caso(GeneradorFalso({ "" }))
        assertEquals(
            ContextoPrediccion.SinPrediccion(MotivoSinPrediccion.MODELO_NO_DISPONIBLE, emptyList()),
            caso.contextoDe(tarea)
        )
        coEvery { predecir(any(), any()) } returns RespuestaApi.Http(401, "{\"detail\":\"Token inválido\"}")
        assertTrue(caso.contextoDe(tarea) is ContextoPrediccion.SinPrediccion)
    }

    // BIBLIOGRAFÍA

    @Test
    fun `solo se muestran las fuentes que el verificador comprueba`() = runTest {
        val verificador = VerificadorFalso(setOf("Cálculo de una variable"))
        val r = (caso(GeneradorFalso({ respuestaConModelo }), verificador)(tarea) as ResultadoRecomendacion.Exito).recomendacion

        // La de tipo desconocido ni siquiera se intenta comprobar
        assertEquals(listOf("Cálculo de una variable", "Inventada"), verificador.recibidas.map { it.titulo })
        assertEquals(listOf("Cálculo de una variable"), r.fuentes.map { it.titulo })
    }

    @Test
    fun `si la verificacion falla la recomendacion sale sin fuentes`() = runTest {
        val rota = object : VerificadorFuentes {
            override suspend fun verificar(propuestas: List<FuentePropuesta>): List<Fuente> = throw IllegalStateException("cuota")
        }
        val r = (caso(GeneradorFalso({ respuestaConModelo }), rota)(tarea) as ResultadoRecomendacion.Exito).recomendacion
        assertTrue(r.fuentes.isEmpty())
    }

    // ERRORES Y CACHÉ

    @Test
    fun `acepta el JSON envuelto en un bloque de codigo y rechaza respuestas incompletas`() {
        val caso = caso(GeneradorFalso({ "" }))

        assertEquals("Mapas mentales", caso.interpretar("```json\n$respuestaConModelo\n```")?.metodo)
        assertNull(caso.interpretar("no es json"))
        assertNull(caso.interpretar("{}"))
        assertNull(caso.interpretar("""{"metodo":"Pomodoro","intro":"Hola","consejo":"","comoAplicar":[]}"""))
    }

    @Test
    fun `reutiliza la recomendacion de la misma tarea salvo que se pida otra`() = runTest {
        val generador = GeneradorFalso({ respuestaConModelo })
        val caso = caso(generador)

        caso(tarea)
        caso(tarea)
        assertEquals(1, generador.llamadas)

        caso(tarea, nueva = true)
        assertEquals(2, generador.llamadas)

        caso(tarea.copy(descrip = "Cinco ejercicios"))
        assertEquals(3, generador.llamadas)
    }

    @Test
    fun `con una prediccion ya hecha no vuelve a llamar a la API y la pasa a Gemini`() = runTest {
        val generador = GeneradorFalso({ respuestaConModelo })
        val caso = caso(generador)
        val contexto = caso.contextoDeRespuesta(datos, RespuestaApi.Http(200, respuestaApi))

        val r = (caso.conContexto(tarea, contexto) as ResultadoRecomendacion.Exito).recomendacion

        coVerify(exactly = 0) { predecir(any(), any()) }
        assertTrue(contexto is ContextoPrediccion.ConPrediccion)
        assertEquals("34 %", r.confianza)
        assertTrue(generador.prompts.single().contains("34 %"))
        // Misma tarea y mismo contexto: se reutiliza
        caso.conContexto(tarea, contexto)
        assertEquals(1, generador.llamadas)
    }

    @Test
    fun `si la API respondio con error el contexto dice que el modelo no estuvo disponible`() {
        val contexto = caso(GeneradorFalso()).contextoDeRespuesta(datos, RespuestaApi.Http(502, "Bad Gateway"))

        assertEquals(ContextoPrediccion.SinPrediccion(MotivoSinPrediccion.MODELO_NO_DISPONIBLE, emptyList()), contexto)
    }

    @Test
    fun `distingue la falta de conexion de otros errores`() = runTest {
        val sinRed = caso(GeneradorFalso({ throw RuntimeException("fallo", IOException("sin red")) }))
        val otro = caso(GeneradorFalso({ throw IllegalStateException("cuota") }))
        val basura = caso(GeneradorFalso({ "lo siento" }))

        assertEquals(ResultadoRecomendacion.SinConexion, sinRed(tarea))
        assertEquals(ResultadoRecomendacion.Error, otro(tarea))
        assertEquals(ResultadoRecomendacion.Error, basura(tarea))
    }

    @Test
    fun `el prompt incluye los datos de la tarea y el plazo`() = runTest {
        val caso = caso(GeneradorFalso({ "" }))
        val contexto = caso.contextoDe(tarea)

        val prompt = caso.construirPrompt(tarea, contexto)
        assertTrue(prompt.contains("Cálculo"))
        assertTrue(prompt.contains("Prioridad: alta"))
        assertTrue(prompt.contains("Faltan 8 días"))

        assertTrue(caso.construirPrompt(tarea.copy(fecha = "2026-11-10"), contexto).contains("ya pasó hace 2 días"))
        assertTrue(caso.construirPrompt(tarea.copy(calificacion = 9f), contexto).contains("calificada"))
    }
}
