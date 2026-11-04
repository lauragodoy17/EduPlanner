package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.database.entities.AsistenciaEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ConfirmarAsistenciaUseCaseTest {

    @RelaxedMockK private lateinit var asignaturasRepository: AsignaturasRepository
    @RelaxedMockK private lateinit var repository: GamificacionRepository
    @RelaxedMockK private lateinit var sesion: SesionUsuario

    private val fecha = LocalDate.of(2026, 10, 28)
    private val reloj = RelojFijo(fecha.atTime(17, 0))
    private val asignatura = Asignatura(
        id = 1, nombre = "Física", tutor = "Ana", duracion = 2, hora = "14:00",
        horario = List(7) { it == fecha.dayOfWeek.value - 1 },
        fechaRegistro = LocalDateTime.of(2026, 10, 1, 8, 0).millis()
    )

    private lateinit var confirmar: ConfirmarAsistenciaUseCase

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "usuario-1"
        coEvery { asignaturasRepository.getAsignatura(1) } returns asignatura
        coEvery { repository.asistencia(any(), any(), any()) } returns null
        coEvery { repository.existeMovimiento(any(), any()) } returns false
        coEvery { repository.contarMovimientosDelDia(any(), any(), any()) } returns 0
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns true
        repository.ejecutarTransaccionesDirectamente()

        val otorgar = OtorgarRecompensaUseCase(repository, sesion, reloj)
        confirmar = ConfirmarAsistenciaUseCase(asignaturasRepository, repository, otorgar, sesion, reloj)
    }

    @Test
    fun `5 confirmar antes de que termine la clase se rechaza`() = runBlocking {
        reloj.ahora = fecha.atTime(15, 0)

        assertEquals(ResultadoAsistencia.AunNoTermina, confirmar(1, fecha, true))
        coVerify(exactly = 0) { repository.insertarAsistencia(any()) }
    }

    @Test
    fun `6 confirmar despues del vencimiento se rechaza`() = runBlocking {
        reloj.ahora = LocalDateTime.of(2026, 10, 29, 0, 0)

        assertEquals(ResultadoAsistencia.PlazoVencido, confirmar(1, fecha, true))
        coVerify(exactly = 0) { repository.insertarAsistencia(any()) }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `7 no asisti se registra sin conceder EduCoins`() = runBlocking {
        assertEquals(ResultadoAsistencia.Registrada(asistio = false, monedas = 0), confirmar(1, fecha, false))

        coVerify { repository.insertarAsistencia(match { !it.asistio && it.asignaturaId == 1 && it.fecha == "2026-10-28" }) }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `8 si asisti concede una sola recompensa con su clave`() = runBlocking {
        assertEquals(ResultadoAsistencia.Registrada(asistio = true, monedas = 10), confirmar(1, fecha, true))

        coVerify(exactly = 1) {
            repository.insertarMovimiento("usuario-1", "ASISTENCIA:1:2026-10-28", "ASISTENCIA", 10, any(), any(), "2026-10-28")
        }
    }

    @Test
    fun `9 repetir la solicitud no duplica la recompensa`() = runBlocking {
        //GIVEN la primera respuesta ya está guardada
        coEvery { repository.asistencia("usuario-1", 1, "2026-10-28") } returns
            AsistenciaEntity("usuario-1", 1, "2026-10-28", true, 0, 0, 0)

        //WHEN
        val resultado = confirmar(1, fecha, true)

        //THEN
        assertEquals(ResultadoAsistencia.YaRespondida, resultado)
        coVerify(exactly = 0) { repository.insertarAsistencia(any()) }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una respuesta no se puede cambiar de no a si para cobrar`() = runBlocking {
        coEvery { repository.asistencia("usuario-1", 1, "2026-10-28") } returns
            AsistenciaEntity("usuario-1", 1, "2026-10-28", false, 0, 0, 0)

        assertEquals(ResultadoAsistencia.YaRespondida, confirmar(1, fecha, true))
    }

    @Test
    fun `12 una clase eliminada no genera recompensa`() = runBlocking {
        coEvery { asignaturasRepository.getAsignatura(1) } returns null

        assertEquals(ResultadoAsistencia.ClaseNoExiste, confirmar(1, fecha, true))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una fecha en la que la asignatura no tiene clase se rechaza`() = runBlocking {
        assertEquals(ResultadoAsistencia.ClaseNoExiste, confirmar(1, fecha.plusDays(1), true))
    }

    @Test
    fun `15 si falla al guardar no hay exito ni movimientos`() = runBlocking {
        coEvery { repository.insertarAsistencia(any()) } throws IllegalStateException("disco lleno")

        assertEquals(ResultadoAsistencia.Error, confirmar(1, fecha, true))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `la asistencia guarda copia del horario de la clase`() = runBlocking {
        confirmar(1, fecha, true)

        coVerify {
            repository.insertarAsistencia(match {
                it.inicioMillis == fecha.atTime(14, 0).millis() &&
                    it.finMillis == fecha.atTime(16, 0).millis() &&
                    it.confirmadaMillis == fecha.atTime(17, 0).millis()
            })
        }
    }

    @Test
    fun `sin sesion no se registra nada`() = runBlocking {
        every { sesion.uid() } returns null

        assertEquals(ResultadoAsistencia.SinSesion, confirmar(1, fecha, true))
    }
}
