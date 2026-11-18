package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.PercepcionesRepository
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DatosEstudioUseCasesTest {

    private val asignaturas: AsignaturasRepository = mockk(relaxed = true)
    private val percepciones: PercepcionesRepository = mockk(relaxed = true)
    private val gamificacion: GamificacionRepository = mockk(relaxed = true)
    private val sesion: SesionUsuario = mockk()

    private val hoy = LocalDate.of(2026, 11, 12)
    private val ahora = hoy.atTime(18, 0)
    private val reloj = RelojFijo(ahora)

    private val calculo = Asignatura(3, "Cálculo", "Ana", 2, "8:00", List(7) { it == 0 }, areaModelo = "Matematicas")

    @Before
    fun setUp() {
        every { sesion.uid() } returns "u1"
        gamificacion.ejecutarTransaccionesDirectamente()
        coEvery { asignaturas.getAsignatura(3) } returns calculo
        coEvery { asignaturas.guardarDatosModelo(any(), any(), any(), any()) } returns true
        coEvery { percepciones.guardar(any(), any(), any(), any(), any(), any(), any()) } returns true
        coEvery { gamificacion.existeSesion(any(), any()) } returns false
    }

    // DATOS DE LA ASIGNATURA

    private val guardarDatos get() = GuardarDatosAsignaturaUseCase(asignaturas, percepciones, gamificacion, sesion, reloj)

    @Test
    fun `guarda area, escala, interes del mes y metodo del dia`() = runBlocking {
        val resultado = guardarDatos(3, "Matematicas", 1f, 5f, 7.5f, "Pomodoro")

        assertEquals(ResultadoGuardarDatos.Guardado, resultado)
        coVerify { asignaturas.guardarDatosModelo(3, "Matematicas", 1f, 5f) }
        // Una respuesta de interés por mes: responder otra vez este mes la corrige
        coVerify { percepciones.guardar(ReglasExtraccion.TIPO_INTERES, "INTERES:3:2026-11", 3, null, 7.5f, null, ahora.millis()) }
        coVerify { percepciones.guardar(ReglasExtraccion.TIPO_METODO, "METODO:3:2026-11-12", 3, null, null, "Pomodoro", ahora.millis()) }
    }

    @Test
    fun `sin responder interes ni metodo no se guardan respuestas inventadas`() = runBlocking {
        guardarDatos(3, "Matematicas", null, null, null, null)
        coVerify(exactly = 0) { percepciones.guardar(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `rechaza valores que no son del catalogo o escalas imposibles`() = runBlocking {
        assertTrue(guardarDatos(3, "Matemáticas", null, null, null, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(guardarDatos(3, null, 5f, 1f, null, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(guardarDatos(3, null, 1f, null, null, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(guardarDatos(3, null, null, null, null, "Leer mucho") is ResultadoGuardarDatos.Invalido)
        coVerify(exactly = 0) { asignaturas.guardarDatosModelo(any(), any(), any(), any()) }
    }

    @Test
    fun `sin sesion no se guarda nada`() = runBlocking {
        every { sesion.uid() } returns null
        assertEquals(ResultadoGuardarDatos.SinSesion, guardarDatos(3, "Matematicas", null, null, 5f, null))
    }

    // DIFICULTAD

    @Test
    fun `la dificultad de una tarea se guarda una vez por tarea con su tema`() = runBlocking {
        val caso = GuardarDificultadUseCase(asignaturas, percepciones, reloj)
        val tarea = Tarea(7, "Taller", "", "Cálculo", "2026-11-20", 1, tema = "Integrales", asignaturaId = 3)

        assertEquals(ResultadoGuardarDatos.Guardado, caso.deTarea(tarea, 7.5f))

        coVerify { percepciones.guardar(ReglasExtraccion.TIPO_DIFICULTAD, "TAREA:7", 3, "Integrales", 7.5f, null, ahora.millis()) }
        assertTrue(caso.deTarea(tarea, 11f) is ResultadoGuardarDatos.Invalido)
    }

    @Test
    fun `una tarea sin asignatura registrada no guarda dificultad`() = runBlocking {
        every { asignaturas.idPorNombre("Otra") } returns null
        val caso = GuardarDificultadUseCase(asignaturas, percepciones, reloj)
        val tarea = Tarea(7, "Taller", "", "Otra", "2026-11-20", 1)

        assertTrue(caso.deTarea(tarea, 5f) is ResultadoGuardarDatos.Invalido)
    }

    // ESTUDIO MANUAL

    private val registrar get() = RegistrarEstudioManualUseCase(asignaturas, gamificacion, sesion, reloj)

    @Test
    fun `el estudio manual se guarda como sesion terminada de origen MANUAL`() = runBlocking {
        val guardada = slot<SesionEnfoqueEntity>()
        coEvery { gamificacion.insertarSesionEnfoque(capture(guardada)) } returns Unit

        assertEquals(ResultadoGuardarDatos.Guardado, registrar(3, hoy.minusDays(2), 90, "Grupos de estudio"))

        with(guardada.captured) {
            assertEquals(SesionEnfoqueEntity.ORIGEN_MANUAL, origen)
            assertEquals(3, asignaturaId)
            assertEquals("Cálculo", asignatura)
            assertEquals(90, minutos)
            assertEquals("Grupos de estudio", metodo)
            assertEquals(inicioMillis + 90 * 60_000L, completadaMillis)
            assertEquals(hoy.minusDays(2).atTime(18, 0).millis(), completadaMillis)
        }
    }

    @Test
    fun `el estudio de hoy no puede terminar en el futuro`() = runBlocking {
        val guardada = slot<SesionEnfoqueEntity>()
        coEvery { gamificacion.insertarSesionEnfoque(capture(guardada)) } returns Unit

        registrar(3, hoy, 60, null)

        assertTrue(guardada.captured.completadaMillis!! <= ahora.millis())
    }

    @Test
    fun `el estudio manual tiene limites de fecha y duracion`() = runBlocking {
        assertTrue(registrar(3, hoy.plusDays(1), 60, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(registrar(3, hoy.minusDays(8), 60, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(registrar(3, hoy, 2, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(registrar(3, hoy, 301, null) is ResultadoGuardarDatos.Invalido)
        coVerify(exactly = 0) { gamificacion.insertarSesionEnfoque(any()) }
    }
}
