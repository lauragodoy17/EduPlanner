package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.DuenoDatosLocales
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.TareaConCalificacion
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TareasRepositoryTest {

    @RelaxedMockK
    private lateinit var tareasDao: TareasDao

    @RelaxedMockK
    private lateinit var asignaturasDao: AsignaturasDao

    @RelaxedMockK
    private lateinit var sesion: SesionUsuario

    @RelaxedMockK
    private lateinit var dueno: DuenoDatosLocales

    private lateinit var repository: TareasRepository

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "u1"
        every { asignaturasDao.idPorNombre(any(), any()) } returns null
        repository = TareasRepository(tareasDao, asignaturasDao, sesion, dueno)
    }

    @Test
    fun `getTareas convierte las entidades del usuario en modelos con su calificacion`() {
        //GIVEN
        every { tareasDao.getAllTareasConCalificacion("u1") } returns listOf(
            TareaConCalificacion(TareaEntity(7, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 1, uid = "u1"), null),
            TareaConCalificacion(TareaEntity(8, "Taller", "", "Física", "2026-10-29", 2, uid = "u1", tema = "Termodinamica", asignaturaId = 3), 4.5F)
        )

        //WHEN
        val result = repository.getTareas()

        //THEN
        assertEquals(
            listOf(
                Tarea(7, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 1),
                Tarea(8, "Taller", "", "Física", "2026-10-29", 2, calificacion = 4.5F, tema = "Termodinamica", asignaturaId = 3)
            ),
            result
        )
        assertEquals(false, result[0].completada)
        assertEquals(true, result[1].completada)
        // Antes de leer se adoptan las filas sin dueño de versiones anteriores
        verify { dueno.asegurar("u1") }
    }

    @Test
    fun `sin sesion no hay tareas ni se guardan`() = runBlocking {
        every { sesion.uid() } returns null

        assertTrue(repository.getTareas().isEmpty())
        assertEquals(-1L, repository.insertTarea(Tarea(null, "Ensayo", "", "Historia", "2026-10-28", 1)))
        coVerify(exactly = 0) { tareasDao.insertTarea(any()) }
    }

    @Test
    fun `insertTarea guarda la entidad del usuario sin id y enlazada a su asignatura`() = runBlocking {
        //GIVEN
        every { asignaturasDao.idPorNombre("u1", "Historia") } returns 5
        val tarea = Tarea(99, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 2, tema = "Historia moderna")

        //WHEN
        repository.insertTarea(tarea)

        //THEN
        coVerify {
            tareasDao.insertTarea(
                TareaEntity(null, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 2, uid = "u1", tema = "Historia moderna", asignaturaId = 5)
            )
        }
    }

    @Test
    fun `deleteTarea borra la entidad con su id`() = runBlocking {
        //GIVEN
        val tarea = Tarea(3, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 3)

        //WHEN
        repository.deleteTarea(tarea)

        //THEN
        coVerify { tareasDao.deleteTarea(TareaEntity(3, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 3, uid = "u1")) }
    }
}
