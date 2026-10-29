package com.softcg.myapplication.ui.Inicio.Fragments.Home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.softcg.myapplication.core.MainDispatcherRule
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Models.DateFilterItem
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK private lateinit var getTareasUseCase: getTareasUseCase
    @RelaxedMockK private lateinit var getEventosUseCase: getEventosUseCase
    @RelaxedMockK private lateinit var tareasRepository: TareasRepository
    @RelaxedMockK private lateinit var eventosRepository: EventosRepository

    private lateinit var viewModel: HomeViewModel

    private val tareaHoy = Tarea(1, "Ensayo", "", "Historia", "2026-10-28", 1)
    private val tareaManana = Tarea(2, "Taller", "", "Física", "2026-10-29", 2)
    private val eventoHoy = Evento(1, "Feria", "", "2026-10-28", 2)
    private val eventoManana = Evento(2, "Salida", "", "2026-10-29", 3)

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { getTareasUseCase() } returns listOf(tareaHoy, tareaManana)
        every { getEventosUseCase() } returns listOf(eventoHoy, eventoManana)

        viewModel = HomeViewModel(getTareasUseCase, getEventosUseCase)
        viewModel.tareasRepository = tareasRepository
        viewModel.eventosRepository = eventosRepository
    }

    @Test
    fun `sin fecha seleccionada se muestran todas las tareas y eventos`() {
        //WHEN
        viewModel.obtenerTareas()
        viewModel.obtenerEventos()

        //THEN
        assertEquals(listOf(tareaHoy, tareaManana), viewModel._filteredTareas.value)
        assertEquals(listOf(eventoHoy, eventoManana), viewModel._filteredEventos.value)
    }

    @Test
    fun `al elegir una fecha solo quedan las tareas y eventos de ese dia`() {
        //GIVEN
        viewModel.obtenerTareas()
        viewModel.obtenerEventos()

        //WHEN
        viewModel.selectDate(dateItem("2026-10-29"))

        //THEN
        assertEquals(listOf(tareaManana), viewModel._filteredTareas.value)
        assertEquals(listOf(eventoManana), viewModel._filteredEventos.value)
    }

    @Test
    fun `el filtro de fechas genera 31 dias con hoy en el centro`() {
        //WHEN
        viewModel.initializeDateFilter()

        //THEN
        val items = viewModel._dateFilterItems.value!!
        assertEquals(31, items.size)
        assertEquals(15, items.indexOfFirst { it.isToday })
    }

    @Test
    fun `al borrar una tarea se recarga la lista`() {
        //WHEN
        viewModel.deleteTarea(tareaHoy)

        //THEN
        coVerify { tareasRepository.deleteTarea(tareaHoy) }
        assertEquals(listOf(tareaHoy, tareaManana), viewModel._tareas.value)
    }

    private fun dateItem(fecha: String): DateFilterItem {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fecha)!!
        return DateFilterItem(date = date, dayNumber = "", dayName = "")
    }
}
