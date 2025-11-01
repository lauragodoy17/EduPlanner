package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import javax.inject.Inject

class AsignaturasRepository @Inject constructor(private val asignaturasDao: AsignaturasDao){

    fun getAllAsignaturas():List<Asignatura>{
        val entities=asignaturasDao.getAllAsignaturas()
        return entities.map {
            // Clean hora field by removing ALL whitespace (including spaces between digits and colon)
            val cleanHora = it.hora.replace(" ", "").trim()
            Asignatura(id = it.id, nombre = it.nombre, tutor = it.tutor,duracion=it.duracion, hora = cleanHora, horario = listOf(it.lunes,it.martes,it.miercoles,it.jueves,it.viernes,it.sabado,it.domingo), tipoCalificacion = it.tipoCalificacion)
        }
    }

    suspend fun deleteAsignatura(asignatura: Asignatura){
        val entity=
            AsignaturaEntity(id = asignatura.id, nombre = asignatura.nombre, tutor = asignatura.tutor, duracion = asignatura.duracion, hora = asignatura.hora, lunes = asignatura.horario[0], martes = asignatura.horario[1], miercoles = asignatura.horario[2], jueves = asignatura.horario[3], viernes = asignatura.horario[4], sabado = asignatura.horario[5], domingo = asignatura.horario[6], tipoCalificacion = asignatura.tipoCalificacion)
        asignaturasDao.deleteAsignatura(entity)
    }
    suspend fun insertAsignatura(asignatura: Asignatura){
        val entity=
            AsignaturaEntity(id = asignatura.id, nombre = asignatura.nombre, tutor = asignatura.tutor, duracion = asignatura.duracion, hora = asignatura.hora, lunes = asignatura.horario[0], martes = asignatura.horario[1], miercoles = asignatura.horario[2], jueves = asignatura.horario[3], viernes = asignatura.horario[4], sabado = asignatura.horario[5], domingo = asignatura.horario[6], tipoCalificacion = asignatura.tipoCalificacion)
        asignaturasDao.insertAsignatura(entity)
    }
}