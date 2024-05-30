package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.CalificacionesDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.data.database.entities.CalificacionEntity
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import javax.inject.Inject

class CalificacionesRespository @Inject constructor(private val calificacionesDao: CalificacionesDao) {

    fun getAllCalificaciones():List<Calificacion>{
        val entities=calificacionesDao.getAllCalificaciones()
        return entities.map {
            Calificacion(id = it.id, tipo = it.tipo, valor = it.valor, asignatura =it.asignatura, descripcion = it.descripcion)
        }
    }
    suspend fun deleteCalificacion(calificacion: Calificacion){
        val entity=
            CalificacionEntity(id = calificacion.id, tipo = calificacion.tipo, valor = calificacion.valor, asignatura = calificacion.asignatura, descripcion = calificacion.descripcion)
        calificacionesDao.deleteCalificacion(entity)
    }
    suspend fun insertCalificacion(calificacion: Calificacion){
        val entity=
            CalificacionEntity(id = calificacion.id, tipo = calificacion.tipo, valor = calificacion.valor, asignatura = calificacion.asignatura, descripcion = calificacion.descripcion)
        calificacionesDao.insertCalificacion(entity)
    }

}