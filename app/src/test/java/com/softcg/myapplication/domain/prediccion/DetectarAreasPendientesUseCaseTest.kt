package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectarAreasPendientesUseCaseTest {

    private val repository: AsignaturasRepository = mockk()
    private val useCase = DetectarAreasPendientesUseCase(repository)

    private fun asignatura(id: Int, nombre: String, area: String? = null, min: Float? = null, max: Float? = null) =
        Asignatura(id, nombre, "", 0, "", List(7) { false }, areaModelo = area, escalaMin = min, escalaMax = max)

    @Test
    fun `completa solo las que no tienen area y cuyo nombre no deja dudas`() = runBlocking {
        every { repository.getAllAsignaturas() } returns listOf(
            asignatura(1, "Cálculo II", min = 1f, max = 5f),
            asignatura(2, "Historia del arte"),
            asignatura(3, "Ética"),
            asignatura(4, "Química", area = "Quimica"),
            asignatura(5, "Física", area = "Area vieja")
        )
        coEvery { repository.guardarDatosModelo(any(), any(), any(), any()) } returns true

        assertEquals(2, useCase())

        // Conserva la escala de notas que ya tenía
        coVerify { repository.guardarDatosModelo(1, "Matematicas", 1f, 5f) }
        coVerify { repository.guardarDatosModelo(5, "Fisica", null, null) }
        coVerify(exactly = 0) { repository.guardarDatosModelo(2, any(), any(), any()) }
        coVerify(exactly = 0) { repository.guardarDatosModelo(3, any(), any(), any()) }
        coVerify(exactly = 0) { repository.guardarDatosModelo(4, any(), any(), any()) }
    }
}
