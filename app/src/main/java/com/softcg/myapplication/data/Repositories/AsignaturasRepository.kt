package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.DuenoDatosLocales
import com.softcg.myapplication.data.database.dao.AsignaturasDao
import com.softcg.myapplication.data.database.dao.CalificacionesDao
import com.softcg.myapplication.data.database.dao.GamificacionDao
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import javax.inject.Inject

class AsignaturasRepository @Inject constructor(
    private val asignaturasDao: AsignaturasDao,
    private val tareasDao: TareasDao,
    private val calificacionesDao: CalificacionesDao,
    private val gamificacionDao: GamificacionDao,
    private val sesion: SesionUsuario,
    private val dueno: DuenoDatosLocales
){

    private fun uid(): String? = sesion.uid()?.also { dueno.asegurar(it) }

    fun getAllAsignaturas():List<Asignatura>{
        val uid = uid() ?: return emptyList()
        return asignaturasDao.getAllAsignaturas(uid).map { it.toModel() }
    }

    suspend fun getAsignatura(id:Int):Asignatura?{
        val uid = uid() ?: return null
        return asignaturasDao.getAsignatura(uid, id)?.toModel()
    }

    fun idPorNombre(nombre: String): Int? {
        val uid = uid() ?: return null
        return asignaturasDao.idPorNombre(uid, nombre)
    }

    suspend fun deleteAsignatura(asignatura: Asignatura){
        val uid = uid() ?: return
        asignaturasDao.deleteAsignatura(asignatura.toEntity(uid))
    }

    /**
     * Devuelve el id de la asignatura guardada (-1 sin sesión). Las nuevas se registran con la hora actual,
     * y lo que ya se había escrito con su nombre (tareas, notas, sesiones de estudio) queda enlazado a su id.
     */
    suspend fun insertAsignatura(asignatura: Asignatura, ahoraMillis: Long = System.currentTimeMillis()): Long{
        val uid = uid() ?: return -1
        val registro = if (asignatura.fechaRegistro == 0L) ahoraMillis else asignatura.fechaRegistro
        val id = asignaturasDao.insertAsignatura(asignatura.copy(fechaRegistro = registro).toEntity(uid))
        if (id > 0) {
            tareasDao.enlazarAsignatura(uid, id.toInt(), asignatura.nombre)
            calificacionesDao.enlazarAsignatura(uid, id.toInt(), asignatura.nombre)
            gamificacionDao.enlazarSesiones(uid, id.toInt(), asignatura.nombre)
        }
        return id
    }

    /** Guarda los datos del formulario (no el área ni la escala). false si no existe o sin sesión. */
    suspend fun actualizarAsignatura(asignatura: Asignatura): Boolean {
        val uid = uid() ?: return false
        val id = asignatura.id ?: return false
        val h = asignatura.horario
        return asignaturasDao.actualizar(
            uid, id, asignatura.nombre, asignatura.tutor, asignatura.duracion, asignatura.hora,
            h[0], h[1], h[2], h[3], h[4], h[5], h[6], asignatura.tipoCalificacion
        ) > 0
    }

    /** Lo que se guardó con el nombre anterior (tareas, notas, sesiones de estudio) pasa al nombre nuevo. */
    suspend fun renombrarEnRegistros(id: Int, viejo: String, nuevo: String) {
        val uid = uid() ?: return
        tareasDao.renombrarAsignatura(uid, id, viejo, nuevo)
        calificacionesDao.renombrarAsignatura(uid, id, viejo, nuevo)
        gamificacionDao.renombrarAsignaturaSesiones(uid, id, viejo, nuevo)
    }

    /** Área del modelo y rango de la escala de notas, que el estudiante confirma. */
    suspend fun guardarDatosModelo(id: Int, area: String?, escalaMin: Float?, escalaMax: Float?): Boolean {
        val uid = uid() ?: return false
        return asignaturasDao.guardarDatosModelo(uid, id, area, escalaMin, escalaMax) > 0
    }

    private fun AsignaturaEntity.toModel(): Asignatura {
        // Clean hora field by removing ALL whitespace (including spaces between digits and colon)
        val cleanHora = hora.replace(" ", "").trim()
        return Asignatura(
            id = id, nombre = nombre, tutor = tutor, duracion = duracion, hora = cleanHora,
            horario = listOf(lunes, martes, miercoles, jueves, viernes, sabado, domingo),
            tipoCalificacion = tipoCalificacion, fechaRegistro = fechaRegistro,
            areaModelo = areaModelo, escalaMin = escalaMin, escalaMax = escalaMax
        )
    }

    private fun Asignatura.toEntity(uid: String) = AsignaturaEntity(
        id = id, nombre = nombre, tutor = tutor, duracion = duracion, hora = hora,
        lunes = horario[0], martes = horario[1], miercoles = horario[2], jueves = horario[3], viernes = horario[4],
        sabado = horario[5], domingo = horario[6], tipoCalificacion = tipoCalificacion, fechaRegistro = fechaRegistro,
        uid = uid, areaModelo = areaModelo, escalaMin = escalaMin, escalaMax = escalaMax
    )
}
