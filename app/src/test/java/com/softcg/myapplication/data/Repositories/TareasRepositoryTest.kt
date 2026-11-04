package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.TareaConCalificacion
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TareasRepositoryTest {

    @RelaxedMockK
    private lateinit var tareasDao: TareasDao

    private lateinit var repository: TareasRepository

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        repository = TareasRepository(tareasDao)
    }

    @Test
    fun `getTareas convierte las entidades en modelos con su calificacion`() {
        //GIVEN
        every { tareasDao.getAllTareasConCalificacion() } returns listOf(
            TareaConCalificacion(TareaEntity(7, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 1), null),
            TareaConCalificacion(TareaEntity(8, "Taller", "", "Física", "2026-10-29", 2), 4.5F)
        )

        //WHEN
        val result = repository.getTareas()

        //THEN
        assertEquals(
            listOf(
                Tarea(7, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 1),
                Tarea(8, "Taller", "", "Física", "2026-10-29", 2, calificacion = 4.5F)
            ),
            result
        )
        assertEquals(false, result[0].completada)
        assertEquals(true, result[1].completada)
    }

    @Test
    fun `insertTarea guarda la entidad sin id para que Room lo genere`() = runBlocking {
        //GIVEN
        val tarea = Tarea(99, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 2)

        //WHEN
        repository.insertTarea(tarea)

        //THEN
        coVerify { tareasDao.insertTarea(TareaEntity(null, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 2)) }
    }

    @Test
    fun `deleteTarea borra la entidad con su id`() = runBlocking {
        //GIVEN
        val tarea = Tarea(3, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 3)

        //WHEN
        repository.deleteTarea(tarea)

        //THEN
        coVerify { tareasDao.deleteTarea(TareaEntity(3, "Ensayo", "Dos páginas", "Historia", "2026-10-28", 3)) }
    }
}
