package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.PercepcionesRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GuardarPercepcionesUseCaseTest {

    private val asignaturas: AsignaturasRepository = mockk(relaxed = true)
    private val percepciones: PercepcionesRepository = mockk(relaxed = true)
    private val gamificacion: GamificacionRepository = mockk(relaxed = true)
    private val sesion: SesionUsuario = mockk()

    private val ahora = LocalDate.of(2026, 11, 12).atTime(18, 0)
    private val caso = GuardarPercepcionesUseCase(asignaturas, percepciones, gamificacion, sesion, RelojFijo(ahora))

    @Before
    fun setUp() {
        every { sesion.uid() } returns "u1"
        gamificacion.ejecutarTransaccionesDirectamente()
        coEvery { asignaturas.getAsignatura(3) } returns Asignatura(3, "Cálculo", "", 0, "", List(7) { false })
        coEvery { percepciones.guardar(any(), any(), any(), any(), any(), any(), any()) } returns true
    }

    @Test
    fun `tiempo y metodo se guardan por separado, sin tocar area ni escala`() = runBlocking {
        assertEquals(ResultadoGuardarDatos.Guardado, caso.estudio(3, 2.5f, "Pomodoro"))

        coVerify { percepciones.guardar(ReglasExtraccion.TIPO_HORAS, "HORAS:3:2026-11", 3, null, 2.5f, null, ahora.millis()) }
        coVerify { percepciones.guardar(ReglasExtraccion.TIPO_METODO, "METODO:3:2026-11-12", 3, null, null, "Pomodoro", ahora.millis()) }
        coVerify(exactly = 0) { asignaturas.guardarDatosModelo(any(), any(), any(), any()) }
    }

    @Test
    fun `responder solo una pregunta guarda solo esa`() = runBlocking {
        caso.estudio(3, null, "Pomodoro")
        coVerify(exactly = 0) { percepciones.guardar(ReglasExtraccion.TIPO_HORAS, any(), any(), any(), any(), any(), any()) }

        caso.interesYDificultad(3, null, 7.5f, null)
        coVerify(exactly = 0) { percepciones.guardar(ReglasExtraccion.TIPO_DIFICULTAD, any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `interes del mes y dificultad del dia en una sola respuesta`() = runBlocking {
        assertEquals(ResultadoGuardarDatos.Guardado, caso.interesYDificultad(3, "Integrales", 7.5f, 5f))

        coVerify { percepciones.guardar(ReglasExtraccion.TIPO_INTERES, "INTERES:3:2026-11", 3, null, 7.5f, null, ahora.millis()) }
        coVerify {
            percepciones.guardar(ReglasExtraccion.TIPO_DIFICULTAD, "DECLARADA:3:Integrales:2026-11-12", 3, "Integrales", 5f, null, ahora.millis())
        }
    }

    @Test
    fun `rechaza respuestas vacias o fuera de rango`() = runBlocking {
        assertTrue(caso.estudio(3, null, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(caso.estudio(3, 41f, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(caso.estudio(3, null, "Leer mucho") is ResultadoGuardarDatos.Invalido)
        assertTrue(caso.interesYDificultad(3, null, null, null) is ResultadoGuardarDatos.Invalido)
        assertTrue(caso.interesYDificultad(3, null, 11f, null) is ResultadoGuardarDatos.Invalido)
        coVerify(exactly = 0) { percepciones.guardar(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sin sesion o sin asignatura no se guarda nada`() = runBlocking {
        coEvery { asignaturas.getAsignatura(9) } returns null
        assertTrue(caso.estudio(9, 2f, null) is ResultadoGuardarDatos.Invalido)

        every { sesion.uid() } returns null
        assertEquals(ResultadoGuardarDatos.SinSesion, caso.interesYDificultad(3, null, 5f, 5f))
        coVerify(exactly = 0) { percepciones.guardar(any(), any(), any(), any(), any(), any(), any()) }
    }
}
