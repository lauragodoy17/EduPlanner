package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.network.SesionUsuario
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class OtorgarRecompensaUseCaseTest {

    @RelaxedMockK private lateinit var repository: GamificacionRepository
    @RelaxedMockK private lateinit var sesion: SesionUsuario

    private val reloj = RelojFijo(LocalDateTime.of(2026, 10, 28, 10, 0))
    private lateinit var otorgar: OtorgarRecompensaUseCase

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "usuario-1"
        coEvery { repository.existeMovimiento(any(), any()) } returns false
        coEvery { repository.contarMovimientosDelDia(any(), any(), any()) } returns 0
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns true
        repository.ejecutarTransaccionesDirectamente()
        otorgar = OtorgarRecompensaUseCase(repository, sesion, reloj)
    }

    @Test
    fun `una actividad nueva concede la cantidad configurada`() = runBlocking {
        assertEquals(ResultadoRecompensa.Otorgada(2), otorgar(TipoRecompensa.TAREA_REGISTRADA, "5"))

        coVerify {
            repository.insertarMovimiento("usuario-1", "TAREA_REGISTRADA:5", "TAREA_REGISTRADA", 2, "Registraste una tarea", any(), "2026-10-28")
        }
    }

    @Test
    fun `la misma actividad no se premia dos veces`() = runBlocking {
        coEvery { repository.existeMovimiento("usuario-1", "TAREA_REGISTRADA:5") } returns true

        assertEquals(ResultadoRecompensa.YaOtorgada, otorgar(TipoRecompensa.TAREA_REGISTRADA, "5"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `si la base rechaza la clave por duplicada no se cuenta como ganada`() = runBlocking {
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns false

        assertEquals(ResultadoRecompensa.YaOtorgada, otorgar(TipoRecompensa.EVENTO_REGISTRADO, "3"))
    }

    @Test
    fun `al alcanzar el tope diario no se conceden mas recompensas de ese tipo`() = runBlocking {
        coEvery { repository.contarMovimientosDelDia("usuario-1", "CLASE_REGISTRADA", "2026-10-28") } returns
            TipoRecompensa.CLASE_REGISTRADA.topeDiario

        assertEquals(ResultadoRecompensa.TopeDiarioAlcanzado, otorgar(TipoRecompensa.CLASE_REGISTRADA, "9"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sin sesion no se otorga nada`() = runBlocking {
        every { sesion.uid() } returns null

        assertEquals(ResultadoRecompensa.SinSesion, otorgar(TipoRecompensa.TAREA_REGISTRADA, "5"))
    }

    @Test
    fun `un error al guardar se informa como error`() = runBlocking {
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } throws IllegalStateException()

        assertEquals(ResultadoRecompensa.Error, otorgar(TipoRecompensa.TAREA_REGISTRADA, "5"))
    }

    @Test
    fun `cada usuario tiene sus propias claves`() = runBlocking {
        every { sesion.uid() } returns "usuario-2"

        otorgar(TipoRecompensa.TAREA_REGISTRADA, "5")

        coVerify { repository.existeMovimiento("usuario-2", "TAREA_REGISTRADA:5") }
        coVerify(exactly = 0) { repository.existeMovimiento("usuario-1", any()) }
    }

    @Test
    fun `las condiciones de cada registro se respetan`() {
        assertEquals(false, CondicionesRecompensa.tareaValida("", "2026-10-28"))
        assertEquals(false, CondicionesRecompensa.eventoValido("Feria", ""))
        assertEquals(false, CondicionesRecompensa.calificacionValida(0f, "Física"))
        assertEquals(false, CondicionesRecompensa.calificacionValida(4f, ""))
        assertEquals(false, CondicionesRecompensa.claseValida("8:00", 2, List(7) { false }))
        assertEquals(false, CondicionesRecompensa.claseValida("", 2, List(7) { true }))
        assertEquals(true, CondicionesRecompensa.claseValida("8: 0", 2, List(7) { it == 0 }))
    }
}
