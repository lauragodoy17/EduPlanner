package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.DuenoDatosLocales
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.TareaConCalificacion
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.TareaEntity
import com.softcg.myapplication.data.network.SesionUsuario
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
        return tareasDao.getAllTareasConCalificacion(uid).map { it.toModel() }
    }

    suspend fun getTarea(id: Int): Tarea? {
        val uid = uid() ?: return null
        return tareasDao.getTarea(uid, id)?.toModel()
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

    private fun TareaConCalificacion.toModel() = Tarea(
        id = tarea.id, titulo = tarea.titulo, descrip = tarea.descrip, asignatura = tarea.asignatura, fecha = tarea.fecha,
        prioridad = tarea.prioridad, calificacion = calificacion, entregadaMillis = tarea.entregadaMillis,
        tema = tarea.tema, asignaturaId = tarea.asignaturaId
    )

    private fun Tarea.toEntity(uid: String) = TareaEntity(
        id = id, titulo = titulo, descrip = descrip, asignatura = asignatura, fecha = fecha, prioridad = prioridad,
        entregadaMillis = entregadaMillis, uid = uid, tema = tema, asignaturaId = asignaturaId
    )
}
