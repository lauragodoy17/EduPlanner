package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import javax.inject.Inject

class AsignaturasRepository @Inject constructor(private val asignaturasDao: AsignaturasDao){

    fun getAllAsignaturas():List<Asignatura>{
        return asignaturasDao.getAllAsignaturas().map { it.toModel() }
    }

    suspend fun getAsignatura(id:Int):Asignatura?{
        return asignaturasDao.getAsignatura(id)?.toModel()
    }

    suspend fun deleteAsignatura(asignatura: Asignatura){
        asignaturasDao.deleteAsignatura(asignatura.toEntity())
    }

    /** Devuelve el id de la asignatura guardada. Las nuevas se registran con la hora actual. */
    suspend fun insertAsignatura(asignatura: Asignatura, ahoraMillis: Long = System.currentTimeMillis()): Long{
        val registro = if (asignatura.fechaRegistro == 0L) ahoraMillis else asignatura.fechaRegistro
        return asignaturasDao.insertAsignatura(asignatura.copy(fechaRegistro = registro).toEntity())
    }

    private fun AsignaturaEntity.toModel(): Asignatura {
        // Clean hora field by removing ALL whitespace (including spaces between digits and colon)
        val cleanHora = hora.replace(" ", "").trim()
        return Asignatura(id = id, nombre = nombre, tutor = tutor, duracion = duracion, hora = cleanHora, horario = listOf(lunes, martes, miercoles, jueves, viernes, sabado, domingo), tipoCalificacion = tipoCalificacion, fechaRegistro = fechaRegistro)
    }

    private fun Asignatura.toEntity() =
        AsignaturaEntity(id = id, nombre = nombre, tutor = tutor, duracion = duracion, hora = hora, lunes = horario[0], martes = horario[1], miercoles = horario[2], jueves = horario[3], viernes = horario[4], sabado = horario[5], domingo = horario[6], tipoCalificacion = tipoCalificacion, fechaRegistro = fechaRegistro)
}
