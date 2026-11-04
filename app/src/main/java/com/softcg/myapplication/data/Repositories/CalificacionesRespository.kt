package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.CalificacionesDao
import com.softcg.myapplication.data.database.entities.CalificacionEntity
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import javax.inject.Inject

class CalificacionesRespository @Inject constructor(private val calificacionesDao: CalificacionesDao) {

    fun getAllCalificaciones():List<Calificacion>{
        val entities=calificacionesDao.getAllCalificaciones()
        return entities.map {
            Calificacion(id = it.id, tipo = it.tipo, valor = it.valor, asignatura =it.asignatura, descripcion = it.descripcion, tareaId = it.tareaId)
        }
    }
    suspend fun deleteCalificacion(calificacion: Calificacion){
        calificacionesDao.deleteCalificacion(calificacion.toEntity())
    }

    /** Devuelve el id de la calificación guardada. */
    suspend fun insertCalificacion(calificacion: Calificacion): Long{
        return calificacionesDao.insertCalificacion(calificacion.toEntity())
    }

    suspend fun tareaTieneCalificacion(tareaId: Int): Boolean{
        return calificacionesDao.contarDeTarea(tareaId) > 0
    }

    private fun Calificacion.toEntity() =
        CalificacionEntity(id = id, tipo = tipo, valor = valor, asignatura = asignatura, descripcion = descripcion, tareaId = tareaId)
}
