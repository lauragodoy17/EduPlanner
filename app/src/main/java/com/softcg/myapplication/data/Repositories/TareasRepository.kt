package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.DuenoDatosLocales
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.TareaConCalificacion
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import javax.inject.Inject

/** Cada usuario solo lee y escribe sus tareas. Sin sesión no hay tareas. */
class TareasRepository @Inject constructor(
    private val tareasDao: TareasDao,
    private val asignaturasDao: AsignaturasDao,
    private val sesion: SesionUsuario,
    private val dueno: DuenoDatosLocales
){

    private fun uid(): String? = sesion.uid()?.also { dueno.asegurar(it) }

    fun getTareas():List<Tarea>{
        val uid = uid() ?: return emptyList()
        return conEscala(uid, tareasDao.getAllTareasConCalificacion(uid).map { it.toModel() })
    }

    suspend fun getTarea(id: Int): Tarea? {
        val uid = uid() ?: return null
        val tarea = tareasDao.getTarea(uid, id)?.toModel() ?: return null
        return conEscala(uid, listOf(tarea)).first()
    }

    /** false si la tarea ya estaba entregada. */
    suspend fun marcarEntregada(id: Int, millis: Long): Boolean {
        val uid = uid() ?: return false
        return tareasDao.marcarEntregada(uid, id, millis) > 0
    }

    suspend fun deleteTarea(tarea: Tarea){
        val uid = uid() ?: return
        tareasDao.deleteTarea(tarea.toEntity(uid))
    }

    /** Devuelve el id de la tarea creada (-1 sin sesión). La asignatura se enlaza por id si existe con ese nombre. */
    suspend fun insertTarea(tarea: Tarea): Long{
        val uid = uid() ?: return -1
        val asignaturaId = tarea.asignaturaId ?: asignaturasDao.idPorNombre(uid, tarea.asignatura)
        return tareasDao.insertTarea(tarea.copy(id = null, asignaturaId = asignaturaId).toEntity(uid))
    }

    /** Guarda lo editado; la asignatura se vuelve a enlazar por su nombre. false si no existe o sin sesión. */
    suspend fun updateTarea(tarea: Tarea): Boolean {
        val uid = uid() ?: return false
        val id = tarea.id ?: return false
        val asignaturaId = asignaturasDao.idPorNombre(uid, tarea.asignatura)
        return tareasDao.actualizar(uid, id, tarea.titulo, tarea.descrip, tarea.asignatura, asignaturaId, tarea.fecha, tarea.prioridad, tarea.horaEntrega) > 0
    }

    /** Las tareas calificadas llevan la escala de su asignatura, para mostrar la nota ("4,5", "A"). */
    private fun conEscala(uid: String, tareas: List<Tarea>): List<Tarea> {
        if (tareas.none { it.calificacion != null }) return tareas
        val asignaturas = asignaturasDao.getAllAsignaturas(uid)
        val porId = asignaturas.associate { it.id to EscalaCalificacion.deAsignatura(it.tipoCalificacion, it.escalaMin, it.escalaMax) }
        val porNombre = asignaturas.associate { it.nombre to EscalaCalificacion.deAsignatura(it.tipoCalificacion, it.escalaMin, it.escalaMax) }
        return tareas.map { tarea ->
            if (tarea.calificacion == null) tarea
            else tarea.copy(escalaNota = tarea.asignaturaId?.let { porId[it] } ?: porNombre[tarea.asignatura])
        }
    }

    private fun TareaConCalificacion.toModel() = Tarea(
        id = tarea.id, titulo = tarea.titulo, descrip = tarea.descrip, asignatura = tarea.asignatura, fecha = tarea.fecha,
        prioridad = tarea.prioridad, calificacion = calificacion, entregadaMillis = tarea.entregadaMillis,
        tema = tarea.tema, asignaturaId = tarea.asignaturaId, horaEntrega = tarea.horaEntrega
    )

    private fun Tarea.toEntity(uid: String) = TareaEntity(
        id = id, titulo = titulo, descrip = descrip, asignatura = asignatura, fecha = fecha, prioridad = prioridad,
        entregadaMillis = entregadaMillis, uid = uid, tema = tema, asignaturaId = asignaturaId, horaEntrega = horaEntrega
    )
}
