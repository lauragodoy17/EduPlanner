package com.softcg.myapplication.domain

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.domain.gamificacion.OtorgarRecompensaUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoRecompensa
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CrearAsignaturaUseCaseTest {

    private val repository: AsignaturasRepository = mockk()
    private val otorgarRecompensa: OtorgarRecompensaUseCase = mockk()
    private val useCase = CrearAsignaturaUseCase(repository, otorgarRecompensa)

    init {
        every { repository.getAllAsignaturas() } returns emptyList()
    }

    @Test
    fun `los dias elegidos se convierten en el horario de lunes a domingo`() {
        assertEquals(
            listOf(true, false, true, false, false, false, false),
            CrearAsignaturaUseCase.horario(listOf("Lunes", "miércoles"))
        )
        assertEquals(List(7) { false }, CrearAsignaturaUseCase.horario(emptyList()))
    }

    @Test
    fun `una clase completa se guarda limpia y se premia`() = runBlocking {
        val guardada = slot<Asignatura>()
        coEvery { repository.insertAsignatura(capture(guardada), any()) } returns 7L
        coEvery { otorgarRecompensa(TipoRecompensa.CLASE_REGISTRADA, "7") } returns ResultadoRecompensa.Otorgada(5)

        val resultado = useCase(NuevaAsignatura("  Física ", "Ana", 2, "08:00", listOf("Lunes")))

        assertEquals(ResultadoCrearAsignatura.Creada(7, "Física", 5, "Fisica"), resultado)
        assertEquals("Física", guardada.captured.nombre)
        assertEquals(listOf(true, false, false, false, false, false, false), guardada.captured.horario)
        assertEquals("Fisica", guardada.captured.areaModelo)
    }

    @Test
    fun `si el nombre no deja claro el area se guarda sin ella para preguntarla`() = runBlocking {
        val guardada = slot<Asignatura>()
        coEvery { repository.insertAsignatura(capture(guardada), any()) } returns 4L

        val resultado = useCase(NuevaAsignatura("Historia del arte"))

        assertEquals(ResultadoCrearAsignatura.Creada(4, "Historia del arte", null, null), resultado)
        assertEquals(null, guardada.captured.areaModelo)
    }

    @Test
    fun `sin hora ni dias se guarda pero no se premia`() = runBlocking {
        coEvery { repository.insertAsignatura(any(), any()) } returns 3L

        val resultado = useCase(NuevaAsignatura("Química"))

        assertEquals(ResultadoCrearAsignatura.Creada(3, "Química", null, "Quimica"), resultado)
        coVerify(exactly = 0) { otorgarRecompensa(any(), any()) }
    }

    @Test
    fun `un nombre vacio no se guarda`() = runBlocking {
        assertEquals(ResultadoCrearAsignatura.NombreVacio, useCase(NuevaAsignatura("   ")))
        coVerify(exactly = 0) { repository.insertAsignatura(any(), any()) }
    }

    @Test
    fun `sin sesion devuelve error`() = runBlocking {
        coEvery { repository.insertAsignatura(any(), any()) } returns -1L

        assertEquals(ResultadoCrearAsignatura.Error, useCase(NuevaAsignatura("Física")))
    }

    @Test
    fun `no se crea una asignatura con el nombre de otra, sin distinguir tildes ni mayusculas`() = runBlocking {
        every { repository.getAllAsignaturas() } returns listOf(Asignatura(1, "Física", "", 0, "", List(7) { false }))

        assertEquals(ResultadoCrearAsignatura.NombreRepetido, useCase(NuevaAsignatura("  fisica ")))
        coVerify(exactly = 0) { repository.insertAsignatura(any(), any()) }
    }
}
