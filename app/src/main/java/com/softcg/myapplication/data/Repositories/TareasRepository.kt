package com.softcg.myapplication.data.Repositories

import androidx.lifecycle.LiveData
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity
import com.softcg.myapplication.ui.tarea.model.Tarea
import javax.inject.Inject

class TareasRepository @Inject constructor(private val tareasDao: TareasDao){

    fun getTareas():List<Tarea>{
        val entities=tareasDao.getAllTareas()
        return  entities.map {
            Tarea(titulo = it.titulo, descrip = it.descrip, asignatura = it.asignatura)
        }
    }

    suspend fun insertTarea(tarea: Tarea){
        val entity = TareaEntity(titulo = tarea.titulo, descrip = tarea.descrip, asignatura = tarea.asignatura)
        tareasDao.insertTarea(entity)
    }
}