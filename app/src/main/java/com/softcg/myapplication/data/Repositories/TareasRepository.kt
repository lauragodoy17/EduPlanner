package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import javax.inject.Inject

class TareasRepository @Inject constructor(private val tareasDao: TareasDao){

    fun getTareas():List<Tarea>{
        val entities=tareasDao.getAllTareasConCalificacion()
        return  entities.map { (t, calificacion) ->
            Tarea(id = t.id,titulo = t.titulo, descrip = t.descrip, asignatura = t.asignatura, fecha = t.fecha, prioridad = t.prioridad, calificacion = calificacion, entregadaMillis = t.entregadaMillis)
        }
    }

    /** false si la tarea ya estaba entregada. */
    suspend fun marcarEntregada(id: Int, millis: Long): Boolean = tareasDao.marcarEntregada(id, millis) > 0

    suspend fun deleteTarea(tarea: Tarea){
        val entity = TareaEntity(id = tarea.id, titulo = tarea.titulo, descrip = tarea.descrip, asignatura = tarea.asignatura, fecha = tarea.fecha, prioridad = tarea.prioridad)
        tareasDao.deleteTarea(entity)
    }

    /** Devuelve el id de la tarea creada. */
    suspend fun insertTarea(tarea: Tarea): Long{
        val entity = TareaEntity(titulo = tarea.titulo, descrip = tarea.descrip, asignatura = tarea.asignatura, fecha = tarea.fecha, prioridad = tarea.prioridad)
        return tareasDao.insertTarea(entity)
    }
}