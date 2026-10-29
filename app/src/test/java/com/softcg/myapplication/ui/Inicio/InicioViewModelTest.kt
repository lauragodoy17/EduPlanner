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
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private lateinit var viewModel: InicioViewModel

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        viewModel = InicioViewModel(getTareasUseCase, getEventosUseCase, getAsignaturasUseCase, getCalificacionesUseCase)
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
    fun `onTareaSelected emite un evento de un solo uso`() {
        //WHEN
        viewModel.onTareaSelected()

        //THEN
        val event = viewModel.navigateToTarea.value!!
        assertEquals(true, event.getContentIfNotHandled())
        assertTrue(event.getContentIfNotHandled() == null)
    }
}
