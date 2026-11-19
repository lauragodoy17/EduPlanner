package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.DuenoDatosLocales
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.CalificacionesDao
import com.softcg.myapplication.data.database.entities.CalificacionEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import javax.inject.Inject

class CalificacionesRespository @Inject constructor(
    private val calificacionesDao: CalificacionesDao,
    private val asignaturasDao: AsignaturasDao,
    private val sesion: SesionUsuario,
    private val dueno: DuenoDatosLocales
) {

    private fun uid(): String? = sesion.uid()?.also { dueno.asegurar(it) }

    fun getAllCalificaciones():List<Calificacion>{
        val uid = uid() ?: return emptyList()
        return conEscala(uid, calificacionesDao.getAllCalificaciones(uid).map { it.toModel() })
    }

    suspend fun deAsignatura(asignaturaId: Int, nombre: String): List<Calificacion> {
        val uid = uid() ?: return emptyList()
        return conEscala(uid, calificacionesDao.deAsignatura(uid, asignaturaId, nombre).map { it.toModel() })
    }

    /** Cuántas notas tiene la asignatura: con una sola, su escala ya no se puede cambiar. */
    suspend fun contarDeAsignatura(asignaturaId: Int, nombre: String): Int {
        val uid = uid() ?: return 0
        return calificacionesDao.contarDeAsignatura(uid, asignaturaId, nombre)
    }

    suspend fun deleteCalificacion(calificacion: Calificacion){
        val uid = uid() ?: return
        calificacionesDao.deleteCalificacion(calificacion.toEntity(uid))
    }

    /** Devuelve el id de la calificación guardada (-1 sin sesión). */
    suspend fun insertCalificacion(calificacion: Calificacion): Long{
        val uid = uid() ?: return -1
        val asignaturaId = calificacion.asignaturaId ?: asignaturasDao.idPorNombre(uid, calificacion.asignatura)
        return calificacionesDao.insertCalificacion(calificacion.copy(asignaturaId = asignaturaId).toEntity(uid))
    }

    suspend fun getCalificacion(id: Int): Calificacion? {
        val uid = uid() ?: return null
        val calificacion = calificacionesDao.getCalificacion(uid, id)?.toModel() ?: return null
        return conEscala(uid, listOf(calificacion)).first()
    }

    /** Guarda lo editado; la asignatura se vuelve a enlazar por su nombre. */
    suspend fun updateCalificacion(calificacion: Calificacion): Boolean {
        val uid = uid() ?: return false
        val id = calificacion.id ?: return false
        val asignaturaId = asignaturasDao.idPorNombre(uid, calificacion.asignatura)
        return calificacionesDao.actualizar(
            uid, id, calificacion.tipo, calificacion.valor, calificacion.asignatura, asignaturaId, calificacion.descripcion
        ) > 0
    }

    /** La nota de una tarea sigue a la tarea cuando esta cambia de asignatura. */
    suspend fun moverDeTarea(tareaId: Int, asignatura: String) {
        val uid = uid() ?: return
        calificacionesDao.moverDeTarea(uid, tareaId, asignatura, asignaturasDao.idPorNombre(uid, asignatura))
    }

    suspend fun tareaTieneCalificacion(tareaId: Int): Boolean{
        return calificacionesDao.contarDeTarea(tareaId) > 0
    }

    /** Cada nota se lee con la escala de su tipo ("Numérico (1-5)") o, si no la dice, con la de su asignatura. */
    private fun conEscala(uid: String, notas: List<Calificacion>): List<Calificacion> {
        if (notas.isEmpty()) return notas
        val asignaturas = asignaturasDao.getAllAsignaturas(uid)
        val porId = asignaturas.associate { it.id to EscalaCalificacion.deAsignatura(it.tipoCalificacion, it.escalaMin, it.escalaMax) }
        val porNombre = asignaturas.associate { it.nombre to EscalaCalificacion.deAsignatura(it.tipoCalificacion, it.escalaMin, it.escalaMax) }
        return notas.map { nota ->
            val escala = EscalaCalificacion.deTexto(nota.tipo)
                ?: nota.asignaturaId?.let { porId[it] }
                ?: porNombre[nota.asignatura]
            nota.copy(escala = escala)
        }
    }

    private fun CalificacionEntity.toModel() = Calificacion(
        id = id, tipo = tipo, valor = valor, asignatura = asignatura, descripcion = descripcion, tareaId = tareaId, asignaturaId = asignaturaId
    )

    private fun Calificacion.toEntity(uid: String) = CalificacionEntity(
        id = id, tipo = tipo, valor = valor, asignatura = asignatura, descripcion = descripcion, tareaId = tareaId, uid = uid, asignaturaId = asignaturaId
    )
}
