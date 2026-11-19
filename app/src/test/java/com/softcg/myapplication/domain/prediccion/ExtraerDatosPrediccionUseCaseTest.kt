package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.PercepcionesRepository
import com.softcg.myapplication.data.database.entities.AsistenciaEntity
import com.softcg.myapplication.data.database.entities.PercepcionEntity
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ExtraerDatosPrediccionUseCaseTest {

    private val asignaturas: AsignaturasRepository = mockk()
    private val calificaciones: CalificacionesRespository = mockk()
    private val gamificacion: GamificacionRepository = mockk()
    private val percepciones: PercepcionesRepository = mockk()
    private val sesion: SesionUsuario = mockk()

    private val hoy = LocalDate.of(2026, 11, 12)
    private val ahora = hoy.atTime(18, 0)
    private val reloj = RelojFijo(ahora)

    private val calculo = Asignatura(
        3, "Cálculo", "Ana", 2, "8:00", List(7) { it == 0 }, tipoCalificacion = "Numérico (0-10)", areaModelo = "Matematicas"
    )
    private val tarea = Tarea(7, "Taller", "", "Cálculo", "2026-11-20", 1, tema = "Integrales", asignaturaId = 3)

    private val caso = ExtraerDatosPrediccionUseCase(asignaturas, calificaciones, gamificacion, percepciones, sesion, reloj)

    private fun sesionEstudio(inicio: LocalDateTime, minutos: Int, metodo: String?, asignaturaId: Int? = 3, nombre: String = "Cálculo") =
        SesionEnfoqueEntity("u1", inicio.millis(), minutos, nombre, inicio.millis() + minutos * 60_000L, asignaturaId = asignaturaId, metodo = metodo)

    private fun percepcion(tipo: String, dias: Long, valor: Float?, texto: String? = null, tema: String? = null) =
        PercepcionEntity("u1", tipo, "$tipo:$dias", 3, tema, valor, texto, ahora.minusDays(dias).millis())

    @Before
    fun setUp() {
        every { sesion.uid() } returns "u1"
        coEvery { asignaturas.getAsignatura(3) } returns calculo
        every { asignaturas.idPorNombre(any()) } returns null
        // 5 clases respondidas, 4 asistidas: 80 %
        coEvery { gamificacion.asistencias("u1") } returns (1..5).map {
            AsistenciaEntity("u1", 3, hoy.minusDays(it * 7L).toString(), it != 2, 0, 0, 0)
        } + AsistenciaEntity("u1", 9, hoy.toString(), false, 0, 0, 0) // de otra asignatura: no cuenta
        // 2,5 h con 20 días de historial; una sesión antigua solo guardó el nombre de la asignatura
        coEvery { gamificacion.sesionesEstudio("u1") } returns listOf(
            sesionEstudio(ahora.minusDays(19), 60, "Pomodoro", asignaturaId = null),
            sesionEstudio(ahora.minusDays(10), 60, "Pomodoro"),
            sesionEstudio(ahora.minusDays(2), 30, "Mapas mentales"),
            sesionEstudio(ahora.minusDays(1), 600, "Pomodoro", asignaturaId = 9, nombre = "Física")
        )
        coEvery { calificaciones.deAsignatura(3, "Cálculo") } returns listOf(
            Calificacion(1, "Tarea", 8f, "Cálculo", ""),
            Calificacion(2, "Numerico (1-5)", 4f, "Cálculo", "")
        )
        coEvery { percepciones.deAsignatura(3) } returns listOf(
            percepcion(ReglasExtraccion.TIPO_INTERES, 5, 7.5f),
            percepcion(ReglasExtraccion.TIPO_DIFICULTAD, 3, 5f, tema = "Integrales")
        )
    }

    private fun resultado(tarea: Tarea? = this.tarea) = runBlocking {
        (caso.deAsignatura(3, tarea) as Extraccion.Lista).resultado
    }

    private fun item(r: ResultadoExtraccion, item: ItemDato) = r.estados.single { it.item == item }

    @Test
    fun `con todos los datos construye la peticion con los valores reales`() {
        val r = resultado()

        assertTrue(r.completo)
        // Área y tema salen solos: solo quedan notas, estudio e interés y dificultad
        assertEquals(listOf(ItemDato.NOTAS, ItemDato.ESTUDIO, ItemDato.PERCEPCION), r.estados.map { it.item })
        assertEquals(
            DatosPrediccion(
                attendanceAvg = 8.0,       // 80 % / 10
                intensityHours = 0.88,     // 2,5 h / (20 días / 7)
                gradeAvg = 7.75,           // 8/10 y 4 en 1-5 = 7,5
                difficultyPerception = 5.0,
                interestLevel = 7.5,
                subjectName = "Matematicas",
                subjectTopic = "Integrales",
                studyMethod = "Pomodoro"
            ),
            r.datos
        )
        assertEquals(5L, r.diasDesdeInteres)
        assertEquals("0,9 h/semana · Pomodoro", item(r, ItemDato.ESTUDIO).valor)
        assertEquals("Interés 7,5 · Dificultad 5,0", item(r, ItemDato.PERCEPCION).valor)
    }

    @Test
    fun `sin clases respondidas suficientes la asistencia se supone del 100 por ciento y no se pide`() {
        coEvery { gamificacion.asistencias("u1") } returns listOf(AsistenciaEntity("u1", 3, hoy.toString(), false, 0, 0, 0))

        val r = resultado()

        assertTrue(r.completo)
        assertEquals(10.0, r.datos!!.attendanceAvg, 0.001)
        assertTrue("Gemini no debe comentar una asistencia que no se midió", r.datos!!.asistenciaSupuesta)
        // Lo supuesto no viaja a la API como campo propio
        assertFalse(r.datos!!.aJson().contains("asistencia"))
        // El 100 % supuesto está fuera del rango de entrenamiento, pero no es un dato del estudiante: no se avisa
        assertTrue(r.avisos.none { it.contains("asistencia") })
    }

    @Test
    fun `un tiempo de estudio fuera del rango de entrenamiento avisa pero no se cambia`() {
        val r = resultado()
        assertTrue(r.avisos.any { it.contains("tiempo de estudio") })
        assertEquals(0.88, r.datos!!.intensityHours, 0.001)
    }

    @Test
    fun `sin area solo entonces aparece el item del area y no hay peticion`() {
        coEvery { asignaturas.getAsignatura(3) } returns calculo.copy(areaModelo = null)

        val r = resultado()

        assertFalse(r.completo)
        assertNull(r.datos)
        val area = item(r, ItemDato.AREA)
        assertFalse(area.listo)
        assertEquals(AccionDato.AREA, area.accion)
        assertTrue(area.detalle.contains("Matemáticas"))
        // El tema depende del área: no se muestra un segundo ítem por lo mismo
        assertTrue(r.estados.none { it.item == ItemDato.TEMA })
    }

    @Test
    fun `el tema no se pregunta, sale del titulo de la tarea`() {
        val r = resultado(tarea.copy(tema = null, titulo = "Taller de integrales por partes"))
        assertTrue(r.estados.none { it.item == ItemDato.TEMA })
        assertEquals("Integrales", r.datos!!.subjectTopic)
    }

    @Test
    fun `una tarea sin titulo pide ponerle uno`() {
        val r = resultado(tarea.copy(tema = null, titulo = "  "))
        val tema = item(r, ItemDato.TEMA)
        assertFalse(tema.listo)
        assertNull(tema.accion)
        assertNull(r.datos)
    }

    @Test
    fun `un titulo que no es un tema del catalogo se envia tal cual y se avisa`() {
        val r = resultado(tarea.copy(tema = null, titulo = "Parcial 2"))
        assertTrue(r.completo)
        assertEquals("Parcial 2", r.datos!!.subjectTopic)
        assertTrue(r.avisos.any { it.contains("Parcial 2") })
    }

    @Test
    fun `con pocas notas se pide registrar mas, sin boton para hacerlo desde ahi`() {
        coEvery { calificaciones.deAsignatura(3, "Cálculo") } returns listOf(Calificacion(1, "Tarea", 8f, "Cálculo", ""))

        val r = resultado()

        val notas = item(r, ItemDato.NOTAS)
        assertFalse(notas.listo)
        assertNull(notas.accion)
        assertTrue(notas.detalle.startsWith("Registra más notas en esta asignatura para poder realizar la recomendación"))
        assertNull(r.datos)
    }

    @Test
    fun `notas sin escala piden indicar la escala de la asignatura`() {
        coEvery { asignaturas.getAsignatura(3) } returns calculo.copy(tipoCalificacion = "Letras (A-F)")
        coEvery { calificaciones.deAsignatura(3, "Cálculo") } returns listOf(
            Calificacion(1, "Tarea", 4f, "Cálculo", ""), Calificacion(2, "Tarea", 5f, "Cálculo", "")
        )

        val notas = item(resultado(), ItemDato.NOTAS)

        assertFalse(notas.listo)
        assertEquals(AccionDato.ESCALA, notas.accion)
    }

    @Test
    fun `con menos de dos semanas de sesiones y sin horas declaradas falta el tiempo`() {
        coEvery { gamificacion.sesionesEstudio("u1") } returns listOf(sesionEstudio(ahora.minusDays(3), 60, "Pomodoro"))

        val estudio = item(resultado(), ItemDato.ESTUDIO)

        assertFalse(estudio.listo)
        assertEquals(AccionDato.ESTUDIO, estudio.accion)
        assertTrue(estudio.detalle.startsWith("Falta cuántas horas a la semana"))
    }

    @Test
    fun `las horas declaradas cuentan mientras no haya dos semanas de sesiones`() {
        coEvery { gamificacion.sesionesEstudio("u1") } returns listOf(sesionEstudio(ahora.minusDays(3), 60, "Pomodoro"))
        coEvery { percepciones.deAsignatura(3) } returns listOf(
            percepcion(ReglasExtraccion.TIPO_INTERES, 5, 7.5f),
            percepcion(ReglasExtraccion.TIPO_DIFICULTAD, 3, 5f),
            percepcion(ReglasExtraccion.TIPO_HORAS, 2, 3f)
        )

        val r = resultado()

        val estudio = item(r, ItemDato.ESTUDIO)
        assertTrue(estudio.listo)
        assertEquals("Según lo que nos contaste", estudio.detalle)
        assertEquals(3.0, r.datos!!.intensityHours, 0.001)
    }

    @Test
    fun `con dos semanas de sesiones mandan las sesiones sobre lo declarado`() {
        coEvery { percepciones.deAsignatura(3) } returns listOf(
            percepcion(ReglasExtraccion.TIPO_INTERES, 5, 7.5f),
            percepcion(ReglasExtraccion.TIPO_DIFICULTAD, 3, 5f),
            percepcion(ReglasExtraccion.TIPO_HORAS, 2, 3f)
        )

        val r = resultado()

        assertEquals("Según tus sesiones de estudio", item(r, ItemDato.ESTUDIO).detalle)
        assertEquals(0.88, r.datos!!.intensityHours, 0.001)
    }

    @Test
    fun `sin metodo el item de estudio dice que falta el metodo`() {
        coEvery { gamificacion.sesionesEstudio("u1") } returns listOf(
            sesionEstudio(ahora.minusDays(19), 60, null), sesionEstudio(ahora.minusDays(2), 30, null)
        )

        val estudio = item(resultado(), ItemDato.ESTUDIO)

        assertFalse(estudio.listo)
        assertEquals("Falta con qué método estudias esta asignatura", estudio.detalle)
    }

    @Test
    fun `un interes de hace mas de 60 dias hay que actualizarlo`() {
        coEvery { percepciones.deAsignatura(3) } returns listOf(
            percepcion(ReglasExtraccion.TIPO_INTERES, 70, 7.5f),
            percepcion(ReglasExtraccion.TIPO_DIFICULTAD, 3, 5f)
        )

        val r = resultado()

        val percepcion = item(r, ItemDato.PERCEPCION)
        assertFalse(percepcion.listo)
        assertEquals(AccionDato.PERCEPCION, percepcion.accion)
        assertTrue(percepcion.detalle.contains("actualízala"))
        assertNull(r.datos)
    }

    @Test
    fun `sin dificultad el item de interes y dificultad dice que falta la dificultad`() {
        coEvery { percepciones.deAsignatura(3) } returns listOf(percepcion(ReglasExtraccion.TIPO_INTERES, 5, 7.5f))

        val percepcion = item(resultado(), ItemDato.PERCEPCION)

        assertFalse(percepcion.listo)
        assertEquals("Falta qué tan difícil te parece esta asignatura", percepcion.detalle)
    }

    @Test
    fun `sin tarea evalua solo los datos de la asignatura`() {
        val r = resultado(tarea = null)
        assertEquals(3, r.total)
        assertTrue(r.completo)
        assertNull("Sin tarea no hay tema, así que no hay petición", r.datos)
    }

    @Test
    fun `una tarea sin asignatura registrada no tiene de donde sacar sus datos`() = runBlocking {
        assertEquals(Extraccion.SinAsignatura, caso.deTarea(tarea.copy(asignaturaId = null, asignatura = "Otra")))
    }

    @Test
    fun `sin sesion no se extrae nada`() = runBlocking {
        every { sesion.uid() } returns null
        assertEquals(Extraccion.SinSesion, caso.deAsignatura(3, tarea))
    }
}
