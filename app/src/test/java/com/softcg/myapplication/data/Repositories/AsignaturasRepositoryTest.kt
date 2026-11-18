package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.DuenoDatosLocales
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.CalificacionesDao
import com.softcg.myapplication.data.database.dao.GamificacionDao
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AsignaturasRepositoryTest {

    @RelaxedMockK private lateinit var asignaturasDao: AsignaturasDao
    @RelaxedMockK private lateinit var tareasDao: TareasDao
    @RelaxedMockK private lateinit var calificacionesDao: CalificacionesDao
    @RelaxedMockK private lateinit var gamificacionDao: GamificacionDao
    @RelaxedMockK private lateinit var sesion: SesionUsuario
    @RelaxedMockK private lateinit var dueno: DuenoDatosLocales

    private lateinit var repository: AsignaturasRepository

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "u1"
        repository = AsignaturasRepository(asignaturasDao, tareasDao, calificacionesDao, gamificacionDao, sesion, dueno)
    }

    @Test
    fun `getAllAsignaturas limpia los espacios de la hora y arma el horario semanal`() {
        //GIVEN
        every { asignaturasDao.getAllAsignaturas("u1") } returns listOf(
            AsignaturaEntity(
                id = 1, nombre = "Física", tutor = "Ana", duracion = 2, hora = " 08 : 30 ",
                lunes = true, martes = false, miercoles = true, jueves = false,
                viernes = false, sabado = false, domingo = false, tipoCalificacion = "Numérica",
                uid = "u1", areaModelo = "Fisica", escalaMin = 1f, escalaMax = 5f
            )
        )

        //WHEN
        val asignatura = repository.getAllAsignaturas().single()

        //THEN
        assertEquals("08:30", asignatura.hora)
        assertEquals(listOf(true, false, true, false, false, false, false), asignatura.horario)
        assertEquals("Numérica", asignatura.tipoCalificacion)
        assertEquals("Fisica", asignatura.areaModelo)
        assertEquals(1f, asignatura.escalaMin)
        assertEquals(5f, asignatura.escalaMax)
    }

    @Test
    fun `al crear una asignatura se enlaza lo que ya se habia escrito con su nombre`() = runBlocking {
        coEvery { asignaturasDao.insertAsignatura(any()) } returns 12L

        val id = repository.insertAsignatura(Asignatura(null, "Historia", "Luis", 2, "8:00", List(7) { it == 0 }), 1_000L)

        assertEquals(12L, id)
        coVerify { asignaturasDao.insertAsignatura(match { it.uid == "u1" && it.fechaRegistro == 1_000L }) }
        coVerify { tareasDao.enlazarAsignatura("u1", 12, "Historia") }
        coVerify { calificacionesDao.enlazarAsignatura("u1", 12, "Historia") }
        coVerify { gamificacionDao.enlazarSesiones("u1", 12, "Historia") }
    }
}
