package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import javax.inject.Inject

class TareasRepository @Inject constructor(private val tareasDao: TareasDao){

    fun getTareas():List<Tarea>{
        val entities=tareasDao.getAllTareas()
        return  entities.map {
            Tarea(id = it.id,titulo = it.titulo, descrip = it.descrip, asignatura = it.asignatura, fecha = it.fecha)
        }
    }

    suspend fun deleteTarea(tarea: Tarea){
        val entity = TareaEntity(id = tarea.id, titulo = tarea.titulo, descrip = tarea.descrip, asignatura = tarea.asignatura, fecha = tarea.fecha)
        tareasDao.deleteTarea(entity)
    }

    suspend fun insertTarea(tarea: Tarea){
        val entity = TareaEntity(titulo = tarea.titulo, descrip = tarea.descrip, asignatura = tarea.asignatura, fecha = tarea.fecha)
        tareasDao.insertTarea(entity)
    }
}