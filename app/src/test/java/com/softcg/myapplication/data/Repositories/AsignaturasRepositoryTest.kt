package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AsignaturasRepositoryTest {

    @RelaxedMockK
    private lateinit var asignaturasDao: AsignaturasDao

    private lateinit var repository: AsignaturasRepository

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        repository = AsignaturasRepository(asignaturasDao)
    }

    @Test
    fun `getAllAsignaturas limpia los espacios de la hora y arma el horario semanal`() {
        //GIVEN
        every { asignaturasDao.getAllAsignaturas() } returns listOf(
            AsignaturaEntity(
                id = 1, nombre = "Física", tutor = "Ana", duracion = 2, hora = " 08 : 30 ",
                lunes = true, martes = false, miercoles = true, jueves = false,
                viernes = false, sabado = false, domingo = false, tipoCalificacion = "Numérica"
            )
        )

        //WHEN
        val asignatura = repository.getAllAsignaturas().single()

        //THEN
        assertEquals("08:30", asignatura.hora)
        assertEquals(listOf(true, false, true, false, false, false, false), asignatura.horario)
        assertEquals("Numérica", asignatura.tipoCalificacion)
    }
}
