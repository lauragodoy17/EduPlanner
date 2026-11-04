package com.softcg.myapplication.ui.Inicio

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.softcg.myapplication.core.MainDispatcherRule
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.getCalificacionesUseCase
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.gamificacion.CalificarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.ClaseProgramada
import com.softcg.myapplication.domain.gamificacion.Celebracion
import com.softcg.myapplication.domain.gamificacion.ConfirmarAsistenciaUseCase
import com.softcg.myapplication.domain.gamificacion.EntregarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.EvaluarProgresoUseCase
import com.softcg.myapplication.domain.gamificacion.LogrosCatalogo
import com.softcg.myapplication.domain.gamificacion.ObtenerClasesPorConfirmarUseCase
import com.softcg.myapplication.domain.gamificacion.ObtenerEnfoqueActivoUseCase
import com.softcg.myapplication.domain.gamificacion.SesionEnfoque
import com.softcg.myapplication.core.millis
import com.softcg.myapplication.domain.gamificacion.ObtenerPanelProgresoUseCase
import com.softcg.myapplication.domain.gamificacion.OtorgarRecompensaUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoEntregarTarea
import com.softcg.myapplication.domain.gamificacion.ResultadoRecompensa
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InicioViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK private lateinit var getTareasUseCase: getTareasUseCase
    @RelaxedMockK private lateinit var getEventosUseCase: getEventosUseCase
    @RelaxedMockK private lateinit var getAsignaturasUseCase: getAsignaturasUseCase
    @RelaxedMockK private lateinit var getCalificacionesUseCase: getCalificacionesUseCase
    @RelaxedMockK private lateinit var tareasRepository: TareasRepository
    @RelaxedMockK private lateinit var eventosRepository: EventosRepository
    @RelaxedMockK private lateinit var calificacionesRepository: CalificacionesRespository
    @RelaxedMockK private lateinit var otorgarRecompensa: OtorgarRecompensaUseCase
    @RelaxedMockK private lateinit var calificarTarea: CalificarTareaUseCase
    @RelaxedMockK private lateinit var obtenerClasesPorConfirmar: ObtenerClasesPorConfirmarUseCase
    @RelaxedMockK private lateinit var confirmarAsistencia: ConfirmarAsistenciaUseCase
    @RelaxedMockK private lateinit var entregarTarea: EntregarTareaUseCase
    @RelaxedMockK private lateinit var evaluarProgreso: EvaluarProgresoUseCase
    @RelaxedMockK private lateinit var obtenerPanel: ObtenerPanelProgresoUseCase
    @RelaxedMockK private lateinit var obtenerEnfoqueActivo: ObtenerEnfoqueActivoUseCase
    @RelaxedMockK private lateinit var gamificacionRepository: GamificacionRepository
    @RelaxedMockK private lateinit var sesion: SesionUsuario

    private val reloj = RelojFijo(LocalDateTime.of(2026, 10, 28, 17, 0))

    private lateinit var viewModel: InicioViewModel

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "usuario-1"
        coEvery { otorgarRecompensa(any(), any()) } returns ResultadoRecompensa.YaOtorgada
        viewModel = InicioViewModel(
            getTareasUseCase, getEventosUseCase, getAsignaturasUseCase, getCalificacionesUseCase,
            otorgarRecompensa, calificarTarea, obtenerClasesPorConfirmar, confirmarAsistencia,
            entregarTarea, evaluarProgreso, obtenerPanel, obtenerEnfoqueActivo,
            reloj, gamificacionRepository, sesion
        )
        viewModel.tareasRepository = tareasRepository
        viewModel.eventosRepository = eventosRepository
        viewModel.calificacionesRespository = calificacionesRepository
    }

    @Test
    fun `al agregar una tarea se guarda y se recarga la lista`() {
        //GIVEN
        val guardada = Tarea(1, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 1)
        every { getTareasUseCase() } returns listOf(guardada)

        //WHEN
        viewModel.onAgregarTareaSelected("Ensayo", "Dos páginas", "Historia", "2026-10-28", 1)

        //THEN
        coVerify { tareasRepository.insertTarea(Tarea(null, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 1)) }
        assertEquals(listOf(guardada), viewModel._tareas.value)
    }

    @Test
    fun `al agregar un evento se guardan la hora y la imagen`() {
        //WHEN
        viewModel.onAgregarEventoSelected("Feria", "Ciencias", "2026-10-28", 2, "10:00", "12:00", "content://imagen")

        //THEN
        coVerify {
            eventosRepository.insertEvento(Evento(null, "Feria", "Ciencias", "2026-10-28", 2, "10:00", "12:00", "content://imagen"))
        }
    }

    @Test
    fun `al actualizar un evento se conserva su id`() {
        //WHEN
        viewModel.onActualizarEventoSelected(5, "Feria", "Ciencias", "2026-10-29", 1)

        //THEN
        coVerify { eventosRepository.updateEvento(Evento(5, "Feria", "Ciencias", "2026-10-29", 1)) }
    }

    @Test
    fun `al agregar una calificacion se guarda en el repositorio`() {
        //WHEN
        viewModel.onAgregarCalificacionSelected("Numerico (1-5)", 4.5F, "Física", "Parcial")

        //THEN
        coVerify { calificacionesRepository.insertCalificacion(Calificacion(null, "Numerico (1-5)", 4.5F, "Física", "Parcial")) }
    }

    @Test
    fun `obtenerAsignaturas expone solo los nombres`() {
        //GIVEN
        every { getAsignaturasUseCase() } returns listOf(
            Asignatura(1, "Física", "Ana", 2, "08:00", List(7) { false }),
            Asignatura(2, "Química", "Luis", 1, "10:00", List(7) { false })
        )

        //WHEN
        viewModel.obtenerAsignaturas()

        //THEN
        assertEquals(listOf("Física", "Química"), viewModel._asignaturas.value)
    }

    @Test
    fun `sin asignaturas se ofrece la opcion de agregar una`() {
        //GIVEN
        every { getAsignaturasUseCase() } returns emptyList()

        //WHEN
        viewModel.obtenerAsignaturas()

        //THEN
        assertEquals(listOf("Agregar Asignatura"), viewModel._asignaturas.value)
    }

    @Test
    fun `al registrar una tarea se premia con el id guardado y se avisa la cantidad real`() {
        //GIVEN
        coEvery { tareasRepository.insertTarea(any()) } returns 42L
        coEvery { otorgarRecompensa(TipoRecompensa.TAREA_REGISTRADA, "42") } returns ResultadoRecompensa.Otorgada(2)

        //WHEN
        viewModel.onAgregarTareaSelected("Ensayo", "", "Historia", "2026-10-28", 1)

        //THEN
        assertEquals(2, viewModel.recompensa.value?.getContentIfNotHandled())
    }

    @Test
    fun `si la recompensa no se registro no se muestra ningun aviso`() {
        //GIVEN
        coEvery { tareasRepository.insertTarea(any()) } returns 42L
        coEvery { otorgarRecompensa(any(), any()) } returns ResultadoRecompensa.Error

        //WHEN
        viewModel.onAgregarTareaSelected("Ensayo", "", "Historia", "2026-10-28", 1)

        //THEN
        assertNull(viewModel.recompensa.value)
    }

    @Test
    fun `una tarea sin titulo no recibe recompensa`() {
        //WHEN
        viewModel.onAgregarTareaSelected("", "", "Historia", "2026-10-28", 1)

        //THEN
        coVerify(exactly = 0) { otorgarRecompensa(any(), any()) }
    }

    @Test
    fun `la lista de eventos se recarga despues de guardar, no antes`() {
        //GIVEN
        val guardado = Evento(3, "Feria", "", "2026-10-30", 2)
        coEvery { eventosRepository.insertEvento(any()) } returns 3L
        every { getEventosUseCase() } returns listOf(guardado)

        //WHEN
        viewModel.onAgregarEventoSelected("Feria", "", "2026-10-30", 2)

        //THEN
        coVerifyOrder {
            eventosRepository.insertEvento(any())
            getEventosUseCase()
        }
        assertEquals(listOf(guardado), viewModel._eventos.value)
    }

    @Test
    fun `editar un evento no da recompensa`() {
        //WHEN
        viewModel.onActualizarEventoSelected(5, "Feria", "Ciencias", "2026-10-29", 1)

        //THEN
        coVerify(exactly = 0) { otorgarRecompensa(any(), any()) }
    }

    @Test
    fun `las clases pendientes se preguntan de una en una y las pospuestas se saltan`() = runBlocking {
        //GIVEN
        val fecha = LocalDate.of(2026, 10, 28)
        val primera = ClaseProgramada(1, "Física", fecha, fecha.atTime(12, 0), fecha.atTime(14, 0))
        val segunda = ClaseProgramada(2, "Química", fecha, fecha.atTime(14, 0), fecha.atTime(16, 0))
        coEvery { obtenerClasesPorConfirmar() } returns listOf(primera, segunda)

        //WHEN / THEN
        assertEquals(primera, viewModel.siguienteClasePorConfirmar())
        viewModel.posponerAsistencia(primera)
        assertEquals(segunda, viewModel.siguienteClasePorConfirmar())
        viewModel.posponerAsistencia(segunda)
        assertNull(viewModel.siguienteClasePorConfirmar())
        viewModel.reiniciarPospuestas()
        assertEquals(primera, viewModel.siguienteClasePorConfirmar())
    }

    @Test
    fun `entregar una tarea recarga la lista e informa el resultado`() {
        //GIVEN
        val tarea = Tarea(7, "Ensayo", "", "Historia", "2026-10-28", 1)
        coEvery { entregarTarea(tarea) } returns ResultadoEntregarTarea.Entregada(8, aTiempo = true)

        //WHEN
        viewModel.entregarTarea(tarea)

        //THEN
        coVerify { getTareasUseCase() }
        assertEquals(ResultadoEntregarTarea.Entregada(8, true), viewModel.resultadoEntregarTarea.value?.getContentIfNotHandled())
    }

    @Test
    fun `una sesion de enfoque solo queda por cobrar cuando ya termino`() = runBlocking {
        //GIVEN: el reloj marca las 17:00
        val hace10Min = reloj.ahora.minusMinutes(10).millis()
        val hace30Min = reloj.ahora.minusMinutes(30).millis()

        //WHEN / THEN
        coEvery { obtenerEnfoqueActivo() } returns null
        assertEquals(false, viewModel.hayEnfoquePorCobrar())

        coEvery { obtenerEnfoqueActivo() } returns SesionEnfoque(hace10Min, 25, "")
        assertEquals(false, viewModel.hayEnfoquePorCobrar())

        coEvery { obtenerEnfoqueActivo() } returns SesionEnfoque(hace30Min, 25, "")
        assertEquals(true, viewModel.hayEnfoquePorCobrar())
    }

    @Test
    fun `solo se avisa de las celebraciones que se acaban de conceder`() {
        //GIVEN
        val logro = Celebracion.LogroDesbloqueado(LogrosCatalogo.logros.first(), null)
        coEvery { evaluarProgreso() } returns emptyList()

        //WHEN / THEN
        viewModel.evaluarProgreso()
        assertNull(viewModel.celebraciones.value)

        coEvery { evaluarProgreso() } returns listOf(logro)
        viewModel.evaluarProgreso()
        assertEquals(listOf(logro), viewModel.celebraciones.value?.getContentIfNotHandled())
    }

    @Test
    fun `onTareaSelected emite un evento de un solo uso`() {
        //WHEN
        viewModel.onTareaSelected()

        //THEN
        val event = viewModel.navigateToTarea.value!!
        assertEquals(true, event.getContentIfNotHandled())
        assertTrue(event.getContentIfNotHandled() == null)
    }
}
