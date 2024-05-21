package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.data.database.entities.EventoEntity
import com.softcg.myapplication.ui.evento.model.Evento
import com.softcg.myapplication.ui.home.model.Asignatura
import javax.inject.Inject

class AsignaturasRepository @Inject constructor(private val asignaturasDao: AsignaturasDao){

    fun getAllAsignaturas():List<Asignatura>{
        val entities=asignaturasDao.getAllAsignaturas()
        return entities.map {
            Asignatura(id = it.id, nombre = it.nombre, tutor = it.tutor)
        }
    }

    suspend fun deleteAsignatura(asignatura: Asignatura){
        val entity=
            AsignaturaEntity(id = asignatura.id, nombre = asignatura.nombre, tutor = asignatura.tutor)
        asignaturasDao.deleteAsignatura(entity)
    }
    suspend fun insertAsignatura(asignatura: Asignatura){
        val entity=
            AsignaturaEntity(id = asignatura.id, nombre = asignatura.nombre, tutor = asignatura.tutor)
        asignaturasDao.insertAsignatura(entity)
    }
}