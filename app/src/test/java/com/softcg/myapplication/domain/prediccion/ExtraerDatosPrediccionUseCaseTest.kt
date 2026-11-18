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

    @Test
    fun `con todos los datos construye la peticion con los valores reales`() {
        val r = resultado()

        assertTrue(r.completo)
        assertEquals(8, r.total)
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
    }

    @Test
    fun `un tiempo de estudio fuera del rango de entrenamiento avisa pero no se cambia`() {
        val r = resultado()
        assertTrue(r.avisos.any { it.contains("tiempo de estudio") })
        assertEquals(0.88, r.datos!!.intensityHours, 0.001)
    }

    @Test
    fun `sin area confirmada no hay peticion y se pide confirmarla con una sugerencia`() {
        coEvery { asignaturas.getAsignatura(3) } returns calculo.copy(areaModelo = null)

        val r = resultado()

        assertFalse(r.completo)
        assertNull(r.datos)
        val area = r.estados.single { it.parametro == ParametroModelo.AREA }
        assertFalse(area.listo)
        assertEquals(AccionDato.DATOS_ASIGNATURA, area.accion)
        assertTrue(area.detalle.contains("Matemáticas"))
        // Sin área tampoco hay temas
        assertEquals(AccionDato.DATOS_ASIGNATURA, r.estados.single { it.parametro == ParametroModelo.TEMA }.accion)
    }

    @Test
    fun `el tema no se pregunta, sale del titulo de la tarea`() {
        val r = resultado(tarea.copy(tema = null, titulo = "Taller de integrales por partes"))
        val tema = r.estados.single { it.parametro == ParametroModelo.TEMA }
        assertTrue(tema.listo)
        assertNull(tema.accion)
        assertEquals("Integrales", r.datos!!.subjectTopic)
    }

    @Test
    fun `un titulo que no es un tema del catalogo se envia tal cual y se avisa`() {
        val r = resultado(tarea.copy(tema = null, titulo = "Parcial 2"))
        assertTrue(r.completo)
        assertEquals("Parcial 2", r.datos!!.subjectTopic)
        assertTrue(r.avisos.any { it.contains("Parcial 2") })
    }

    @Test
    fun `con pocas clases o notas faltan esos datos y no se rellenan`() {
        coEvery { gamificacion.asistencias("u1") } returns listOf(AsistenciaEntity("u1", 3, hoy.toString(), true, 0, 0, 0))
        coEvery { calificaciones.deAsignatura(3, "Cálculo") } returns listOf(Calificacion(1, "Tarea", 8f, "Cálculo", ""))

        val r = resultado()

        assertFalse(r.estados.single { it.parametro == ParametroModelo.ASISTENCIA }.listo)
        val notas = r.estados.single { it.parametro == ParametroModelo.NOTAS }
        assertFalse(notas.listo)
        assertEquals(AccionDato.CALIFICACIONES, notas.accion)
        assertNull(r.datos)
    }

    @Test
    fun `notas sin escala piden indicar la escala de la asignatura`() {
        coEvery { asignaturas.getAsignatura(3) } returns calculo.copy(tipoCalificacion = "Letras (A-F)")
        coEvery { calificaciones.deAsignatura(3, "Cálculo") } returns listOf(
            Calificacion(1, "Tarea", 4f, "Cálculo", ""), Calificacion(2, "Tarea", 5f, "Cálculo", "")
        )

        val notas = resultado().estados.single { it.parametro == ParametroModelo.NOTAS }

        assertFalse(notas.listo)
        assertEquals(AccionDato.DATOS_ASIGNATURA, notas.accion)
    }

    @Test
    fun `menos de dos semanas de estudio aun no es suficiente`() {
        coEvery { gamificacion.sesionesEstudio("u1") } returns listOf(sesionEstudio(ahora.minusDays(3), 60, "Pomodoro"))

        val estudio = resultado().estados.single { it.parametro == ParametroModelo.ESTUDIO }

        assertFalse(estudio.listo)
        assertEquals(AccionDato.REGISTRAR_ESTUDIO, estudio.accion)
    }

    @Test
    fun `un interes de hace mas de 60 dias hay que actualizarlo`() {
        coEvery { percepciones.deAsignatura(3) } returns listOf(
            percepcion(ReglasExtraccion.TIPO_INTERES, 70, 7.5f),
            percepcion(ReglasExtraccion.TIPO_DIFICULTAD, 3, 5f)
        )

        val r = resultado()

        val interes = r.estados.single { it.parametro == ParametroModelo.INTERES }
        assertFalse(interes.listo)
        assertEquals(AccionDato.DATOS_ASIGNATURA, interes.accion)
        assertNull(r.datos)
    }

    @Test
    fun `sin tarea evalua solo los 7 datos de la asignatura`() {
        val r = resultado(tarea = null)
        assertEquals(7, r.total)
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
