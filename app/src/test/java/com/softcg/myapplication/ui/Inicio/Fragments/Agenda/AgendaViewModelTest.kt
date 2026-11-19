package com.softcg.myapplication.ui.Inicio.Fragments.Agenda

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.FiltroAgenda
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class AgendaViewModelTest {

    private val viewModel = AgendaViewModel(mockk(relaxed = true), mockk(relaxed = true), RelojFijo(LocalDateTime.of(2026, 11, 19, 10, 0)))

    private val pendiente = Tarea(1, "Ensayo", "", "Historia", "2026-11-20", 1)
    private val entregada = Tarea(2, "Taller", "", "Cálculo", "2026-11-18", 2, entregadaMillis = 5L)
    private val calificada = Tarea(3, "Quiz", "", "Cálculo", "2026-11-17", 2, calificacion = 4.5f)
    private val proximo = Evento(10, "Feria", "", "2026-11-21", 1)
    private val pasado = Evento(11, "Charla", "", "2026-11-10", 1)

    private val tareas = listOf(pendiente, entregada, calificada)
    private val eventos = listOf(proximo, pasado)

    private fun ids(filtro: FiltroAgenda) = viewModel.elementos(tareas, eventos, filtro).map { it.id }.toSet()

    @Test
    fun `por defecto muestra tareas sin entregar y eventos que no han pasado`() {
        assertEquals(FiltroAgenda.SIN_ENTREGAR, viewModel.filtro.value)
        assertEquals(setOf(1, 10), ids(FiltroAgenda.SIN_ENTREGAR))
    }

    @Test
    fun `cada filtro muestra lo suyo`() {
        assertEquals(setOf(1), ids(FiltroAgenda.SOLO_TAREAS))
        assertEquals(setOf(10), ids(FiltroAgenda.SOLO_EVENTOS))
        assertEquals(setOf(2, 3), ids(FiltroAgenda.ENTREGADOS))
    }

    @Test
    fun `las entregadas dicen su estado y su calificacion`() {
        val porId = viewModel.elementos(tareas, eventos, FiltroAgenda.ENTREGADOS).associateBy { it.id }
        assertEquals(true, porId[2]!!.entregada)
        assertEquals(null, porId[2]!!.textoCalificacion)
        assertEquals("4,5", porId[3]!!.textoCalificacion)
    }

    @Test
    fun `el resumen cuenta segun el filtro`() {
        assertEquals("1 pendiente", FiltroAgenda.SIN_ENTREGAR.resumen(1))
        assertEquals("3 tareas entregadas", FiltroAgenda.ENTREGADOS.resumen(3))
        assertEquals("2 eventos próximos", FiltroAgenda.SOLO_EVENTOS.resumen(2))
    }
}
