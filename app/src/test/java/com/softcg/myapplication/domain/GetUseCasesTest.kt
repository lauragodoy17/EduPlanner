package com.softcg.myapplication.domain

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetUseCasesTest {

    @MockK
    private lateinit var tareasRepository: TareasRepository

    @MockK
    private lateinit var eventosRepository: EventosRepository

    @MockK
    private lateinit var asignaturasRepository: AsignaturasRepository

    @MockK
    private lateinit var calificacionesRepository: CalificacionesRespository

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `getTareasUseCase devuelve las tareas del repositorio`() {
        //GIVEN
        val tareas = listOf(Tarea(1, "Ensayo", "Historia", "Historia", "2026-10-28", 1))
        every { tareasRepository.getTareas() } returns tareas

        //WHEN
        val result = getTareasUseCase(tareasRepository)()

        //THEN
        assertEquals(tareas, result)
    }

    @Test
    fun `getEventosUseCase devuelve los eventos del repositorio`() {
        //GIVEN
        val eventos = listOf(Evento(1, "Feria", "Ciencias", "2026-10-28", 2))
        every { eventosRepository.getAllEventos() } returns eventos

        //WHEN
        val result = getEventosUseCase(eventosRepository)()

        //THEN
        assertEquals(eventos, result)
    }

    @Test
    fun `getAsignaturasUseCase devuelve las asignaturas del repositorio`() {
        //GIVEN
        val asignaturas = listOf(Asignatura(1, "Física", "Ana", 2, "08:00", List(7) { false }))
        every { asignaturasRepository.getAllAsignaturas() } returns asignaturas

        //WHEN
        val result = getAsignaturasUseCase(asignaturasRepository)()

        //THEN
        assertEquals(asignaturas, result)
    }

    @Test
    fun `getCalificacionesUseCase devuelve las calificaciones del repositorio`() {
        //GIVEN
        val calificaciones = listOf(Calificacion(1, "Numerico (1-5)", 4.5F, "Física", "Parcial"))
        every { calificacionesRepository.getAllCalificaciones() } returns calificaciones

        //WHEN
        val result = getCalificacionesUseCase(calificacionesRepository)()

        //THEN
        assertEquals(calificaciones, result)
    }
}
