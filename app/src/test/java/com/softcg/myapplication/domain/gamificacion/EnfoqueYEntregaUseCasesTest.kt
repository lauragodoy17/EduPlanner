package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class EnfoqueYEntregaUseCasesTest {

    @RelaxedMockK private lateinit var repository: GamificacionRepository
    @RelaxedMockK private lateinit var tareasRepository: TareasRepository
    @RelaxedMockK private lateinit var sesion: SesionUsuario

    private val inicio = LocalDateTime.of(2026, 11, 3, 10, 0)
    private val reloj = RelojFijo(inicio)
    private val sesionGuardada = SesionEnfoqueEntity("usuario-1", inicio.millis(), 25, "Física", null)

    private lateinit var otorgar: OtorgarRecompensaUseCase
    private lateinit var obtenerActiva: ObtenerEnfoqueActivoUseCase
    private lateinit var iniciar: IniciarEnfoqueUseCase
    private lateinit var completar: CompletarEnfoqueUseCase
    private lateinit var abandonar: AbandonarEnfoqueUseCase
    private lateinit var entregar: EntregarTareaUseCase

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "usuario-1"
        coEvery { repository.sesionEnfoqueActiva(any()) } returns null
        coEvery { repository.existeMovimiento(any(), any()) } returns false
        coEvery { repository.contarMovimientosDelDia(any(), any(), any()) } returns 0
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns true
        coEvery { tareasRepository.marcarEntregada(any(), any()) } returns true
        repository.ejecutarTransaccionesDirectamente()
        otorgar = OtorgarRecompensaUseCase(repository, sesion, reloj)
        obtenerActiva = ObtenerEnfoqueActivoUseCase(repository, sesion, reloj)
        iniciar = IniciarEnfoqueUseCase(obtenerActiva, repository, sesion, reloj)
        completar = CompletarEnfoqueUseCase(otorgar, repository, sesion, reloj)
        abandonar = AbandonarEnfoqueUseCase(repository, sesion)
        entregar = EntregarTareaUseCase(tareasRepository, repository, otorgar, sesion, reloj)
    }

    // Iniciar

    @Test
    fun `iniciar guarda la sesion con la hora real`() = runBlocking {
        val resultado = iniciar(25, "Física")

        assertEquals(ResultadoIniciarEnfoque.Iniciada(SesionEnfoque(inicio.millis(), 25, "Física")), resultado)
        coVerify { repository.insertarSesionEnfoque(sesionGuardada) }
    }

    @Test
    fun `solo se admiten las duraciones del catalogo`() = runBlocking {
        assertEquals(ResultadoIniciarEnfoque.DuracionInvalida, iniciar(1, ""))
        coVerify(exactly = 0) { repository.insertarSesionEnfoque(any()) }
    }

    @Test
    fun `no puede haber dos sesiones a la vez`() = runBlocking {
        coEvery { repository.sesionEnfoqueActiva("usuario-1") } returns sesionGuardada

        assertEquals(ResultadoIniciarEnfoque.YaHayUna, iniciar(25, ""))
        coVerify(exactly = 0) { repository.insertarSesionEnfoque(any()) }
    }

    // Completar

    @Test
    fun `antes de tiempo no se paga aunque la pantalla lo pida`() = runBlocking {
        coEvery { repository.sesionEnfoqueActiva("usuario-1") } returns sesionGuardada
        reloj.ahora = inicio.plusMinutes(24)

        assertEquals(ResultadoCompletarEnfoque.AunNoTermina, completar())
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.completarSesionEnfoque(any(), any(), any()) }
    }

    @Test
    fun `al terminar se paga segun la duracion guardada`() = runBlocking {
        coEvery { repository.sesionEnfoqueActiva("usuario-1") } returns sesionGuardada
        reloj.ahora = inicio.plusMinutes(25)

        assertEquals(ResultadoCompletarEnfoque.Completada(25, TipoRecompensa.ENFOQUE_MEDIO.cantidad), completar())
        coVerify { repository.completarSesionEnfoque("usuario-1", inicio.millis(), any()) }
        coVerify { repository.insertarMovimiento("usuario-1", "ENFOQUE_MEDIO:${inicio.millis()}", "ENFOQUE_MEDIO", 10, any(), any(), any()) }
    }

    @Test
    fun `al superar el tope diario la sesion cuenta pero no paga`() = runBlocking {
        coEvery { repository.sesionEnfoqueActiva("usuario-1") } returns sesionGuardada
        coEvery { repository.contarMovimientosDelDia("usuario-1", "ENFOQUE_MEDIO", any()) } returns TipoRecompensa.ENFOQUE_MEDIO.topeDiario
        reloj.ahora = inicio.plusMinutes(25)

        assertEquals(ResultadoCompletarEnfoque.Completada(25, 0), completar())
        coVerify { repository.completarSesionEnfoque("usuario-1", inicio.millis(), any()) }
    }

    @Test
    fun `una sesion olvidada caduca y no se puede cobrar`() = runBlocking {
        coEvery { repository.sesionEnfoqueActiva("usuario-1") } returns sesionGuardada
        reloj.ahora = inicio.plusMinutes(25).plusHours(13)

        assertEquals(ResultadoCompletarEnfoque.Caducada, completar())
        coVerify { repository.borrarSesionEnfoque("usuario-1", inicio.millis()) }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sin sesion en curso no hay nada que completar`() = runBlocking {
        assertEquals(ResultadoCompletarEnfoque.SinSesionActiva, completar())
    }

    // Reanudar y abandonar

    @Test
    fun `la sesion en curso se recupera de lo guardado`() = runBlocking {
        coEvery { repository.sesionEnfoqueActiva("usuario-1") } returns sesionGuardada
        reloj.ahora = inicio.plusMinutes(10)

        assertEquals(SesionEnfoque(inicio.millis(), 25, "Física"), obtenerActiva())
    }

    @Test
    fun `una sesion caducada se limpia al consultarla`() = runBlocking {
        coEvery { repository.sesionEnfoqueActiva("usuario-1") } returns sesionGuardada
        reloj.ahora = inicio.plusDays(2)

        assertNull(obtenerActiva())
        coVerify { repository.borrarSesionEnfoque("usuario-1", inicio.millis()) }
    }

    @Test
    fun `abandonar borra la sesion y no paga`() = runBlocking {
        coEvery { repository.sesionEnfoqueActiva("usuario-1") } returns sesionGuardada

        assertTrue(abandonar())
        coVerify { repository.borrarSesionEnfoque("usuario-1", inicio.millis()) }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    // Entregar

    private val tarea = Tarea(12, "Ensayo", "", "Historia", "2026-11-03", 1)

    @Test
    fun `entregar antes del vencimiento paga completar y el bono a tiempo`() = runBlocking {
        assertEquals(ResultadoEntregarTarea.Entregada(8, aTiempo = true), entregar(tarea))

        coVerify { tareasRepository.marcarEntregada(12, inicio.millis()) }
        coVerify { repository.insertarMovimiento("usuario-1", "TAREA_COMPLETADA:12", "TAREA_COMPLETADA", 5, any(), any(), any()) }
        coVerify { repository.insertarMovimiento("usuario-1", "TAREA_A_TIEMPO:12", "TAREA_A_TIEMPO", 3, any(), any(), any()) }
    }

    @Test
    fun `entregar una tarea vencida paga completar sin bono`() = runBlocking {
        assertEquals(ResultadoEntregarTarea.Entregada(5, aTiempo = false), entregar(tarea.copy(fecha = "2026-11-02")))

        coVerify(exactly = 0) { repository.insertarMovimiento(any(), "TAREA_A_TIEMPO:12", any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una tarea ya entregada no se paga otra vez`() = runBlocking {
        coEvery { tareasRepository.marcarEntregada(12, any()) } returns false

        assertEquals(ResultadoEntregarTarea.YaEntregada, entregar(tarea))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `si ya se cobro al calificarla, entregarla no vuelve a pagar`() = runBlocking {
        coEvery { repository.existeMovimiento("usuario-1", "TAREA_COMPLETADA:12") } returns true

        assertEquals(ResultadoEntregarTarea.Entregada(0, aTiempo = false), entregar(tarea))
    }

    @Test
    fun `sin id o con un error no se informa como entregada`() = runBlocking {
        assertEquals(ResultadoEntregarTarea.Error, entregar(tarea.copy(id = null)))

        coEvery { tareasRepository.marcarEntregada(any(), any()) } throws IllegalStateException()
        assertEquals(ResultadoEntregarTarea.Error, entregar(tarea))
    }
}
