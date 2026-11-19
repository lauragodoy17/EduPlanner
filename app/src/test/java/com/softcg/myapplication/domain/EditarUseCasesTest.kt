package com.softcg.myapplication.domain

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.domain.calificaciones.TipoEscala
import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion
import com.softcg.myapplication.domain.calificaciones.EscalaBloqueadaUseCase
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class EditarUseCasesTest {

    private val tareas: TareasRepository = mockk(relaxed = true)
    private val calificaciones: CalificacionesRespository = mockk(relaxed = true)
    private val editarTarea = EditarTareaUseCase(tareas, calificaciones)

    private val asignaturas: AsignaturasRepository = mockk(relaxed = true)
    private val gamificacion: GamificacionRepository = mockk()
    private val escalaBloqueada: EscalaBloqueadaUseCase = mockk(relaxed = true)
    private val editarAsignatura = EditarAsignaturaUseCase(asignaturas, gamificacion, escalaBloqueada)

    private val tarea = Tarea(7, "Taller", "", "Cálculo", "2026-11-20", 1, entregadaMillis = 5L)
    private val calculo = Asignatura(1, "Cálculo", "Ana", 2, "08:00", listOf(true) + List(6) { false }, areaModelo = "Matematicas")

    init {
        coEvery { gamificacion.enTransaccion<Boolean>(any()) } coAnswers { firstArg<suspend () -> Boolean>().invoke() }
        every { asignaturas.getAllAsignaturas() } returns listOf(calculo, Asignatura(2, "Física", "", 0, "", List(7) { false }))
        coEvery { asignaturas.getAsignatura(1) } returns calculo
        coEvery { asignaturas.actualizarAsignatura(any()) } returns true
    }

    @Test
    fun `editar una tarea guarda lo escrito sin espacios de sobra`() = runBlocking {
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { tareas.updateTarea(any()) } returns true

        assertEquals(ResultadoEdicion.Guardada, editarTarea(tarea.copy(titulo = "  Taller 2 ", descrip = " ejercicios ")))

        coVerify { tareas.updateTarea(match { it.titulo == "Taller 2" && it.descrip == "ejercicios" }) }
        coVerify(exactly = 0) { calificaciones.moverDeTarea(any(), any()) }
    }

    @Test
    fun `si la tarea cambia de asignatura su nota se va con ella`() = runBlocking {
        coEvery { tareas.getTarea(7) } returns tarea
        coEvery { tareas.updateTarea(any()) } returns true

        editarTarea(tarea.copy(asignatura = "Física"))

        coVerify { calificaciones.moverDeTarea(7, "Física") }
    }

    @Test
    fun `una tarea sin titulo o borrada no se guarda`() = runBlocking {
        assertEquals(ResultadoEdicion.NombreVacio, editarTarea(tarea.copy(titulo = " ")))
        coEvery { tareas.getTarea(7) } returns null
        assertEquals(ResultadoEdicion.NoExiste, editarTarea(tarea))
        coVerify(exactly = 0) { tareas.updateTarea(any()) }
    }

    @Test
    fun `renombrar una asignatura lleva el nombre nuevo a sus tareas, notas y sesiones`() = runBlocking {
        val guardada = slot<Asignatura>()
        coEvery { asignaturas.actualizarAsignatura(capture(guardada)) } returns true

        val r = editarAsignatura(1, NuevaAsignatura(" Cálculo II ", "Ana", 3, "10:00", listOf("Martes")))

        assertEquals(ResultadoEdicion.Guardada, r)
        assertEquals("Cálculo II", guardada.captured.nombre)
        assertEquals(listOf(false, true, false, false, false, false, false), guardada.captured.horario)
        assertEquals("El área confirmada se conserva", "Matematicas", guardada.captured.areaModelo)
        coVerify { asignaturas.renombrarEnRegistros(1, "Cálculo", "Cálculo II") }
    }

    @Test
    fun `sin cambiar el nombre no se tocan los registros`() = runBlocking {
        editarAsignatura(1, NuevaAsignatura("Cálculo", "Luis"))
        coVerify(exactly = 0) { asignaturas.renombrarEnRegistros(any(), any(), any()) }
    }

    @Test
    fun `no se puede usar el nombre de otra asignatura`() = runBlocking {
        assertEquals(ResultadoEdicion.NombreRepetido, editarAsignatura(1, NuevaAsignatura("fisica")))
        assertEquals(ResultadoEdicion.NombreVacio, editarAsignatura(1, NuevaAsignatura("  ")))
        coVerify(exactly = 0) { asignaturas.actualizarAsignatura(any()) }
    }

    @Test
    fun `sin area, el nombre nuevo se usa para detectarla`() = runBlocking {
        coEvery { asignaturas.getAsignatura(1) } returns calculo.copy(nombre = "Clase 1", areaModelo = null)

        editarAsignatura(1, NuevaAsignatura("Química orgánica"))

        coVerify { asignaturas.guardarDatosModelo(1, "Quimica", null, null) }
    }

    @Test
    fun `la escala se cambia al editar solo si la asignatura no tiene notas`() = runBlocking {
        val porcentaje = EscalaCalificacion.de(TipoEscala.PORCENTAJE)
        coEvery { escalaBloqueada(any()) } returns false
        assertEquals(ResultadoEdicion.Guardada, editarAsignatura(1, NuevaAsignatura("Cálculo", escala = porcentaje)))
        coVerify { asignaturas.guardarEscala(1, porcentaje) }

        coEvery { escalaBloqueada(any()) } returns true
        assertEquals(ResultadoEdicion.EscalaBloqueada, editarAsignatura(1, NuevaAsignatura("Cálculo", escala = EscalaCalificacion.de(TipoEscala.LETRAS))))
        coVerify(exactly = 1) { asignaturas.guardarEscala(any(), any()) }
    }
}
