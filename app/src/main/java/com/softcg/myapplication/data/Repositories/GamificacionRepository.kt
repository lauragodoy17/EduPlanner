package com.softcg.myapplication.data.Repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import androidx.room.withTransaction
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import com.softcg.myapplication.data.database.dao.GamificacionDao
import com.softcg.myapplication.data.database.entities.AsistenciaEntity
import com.softcg.myapplication.data.database.entities.AvatarAparienciaEntity
import com.softcg.myapplication.data.database.entities.AvatarEquipadoEntity
import com.softcg.myapplication.data.database.entities.ClaveConFecha
import com.softcg.myapplication.data.database.entities.InventarioEntity
import com.softcg.myapplication.data.database.entities.MisionAsignadaEntity
import com.softcg.myapplication.data.database.entities.MovimientoEduCoinsEntity
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.database.entities.XpPorDia
import com.softcg.myapplication.domain.gamificacion.MovimientoEduCoins
import com.softcg.myapplication.domain.gamificacion.TIPOS_SIN_XP
import javax.inject.Inject

/**
 * Persistencia de EduCoins, asistencias, inventario y avatar, siempre filtrada por usuario.
 * Las reglas de negocio están en los casos de uso de domain/gamificacion.
 */
class GamificacionRepository @Inject constructor(
    private val db: TareasDatabase,
    private val dao: GamificacionDao
) {

    /** Ejecuta [bloque] como una única transacción */
    suspend fun <T> enTransaccion(bloque: suspend () -> T): T = db.withTransaction { bloque() }

    // EduCoins
    fun observarSaldo(uid: String): LiveData<Int> = dao.observarSaldo(uid)

    suspend fun saldo(uid: String): Int = dao.saldo(uid)

    fun observarMovimientos(uid: String): LiveData<List<MovimientoEduCoins>> =
        dao.observarMovimientos(uid).map { lista ->
            lista.map { MovimientoEduCoins(it.tipo, it.cantidad, it.descripcion, it.fechaMillis) }
        }

    suspend fun existeMovimiento(uid: String, clave: String): Boolean = dao.contarClave(uid, clave) > 0

    suspend fun contarMovimientosDelDia(uid: String, tipo: String, dia: String): Int =
        dao.contarPorTipoYDia(uid, tipo, dia)

    /** false si ya existía un movimiento con esa clave (no se duplica). */
    suspend fun insertarMovimiento(
        uid: String, clave: String, tipo: String, cantidad: Int, descripcion: String, fechaMillis: Long, dia: String
    ): Boolean = dao.insertarMovimiento(
        MovimientoEduCoinsEntity(uid = uid, clave = clave, tipo = tipo, cantidad = cantidad, descripcion = descripcion, fechaMillis = fechaMillis, dia = dia)
    ) != -1L

    // Progreso
    fun observarTotalMovimientos(uid: String): LiveData<Int> = dao.observarTotalMovimientos(uid)

    /** Emite cada vez que cambia algo de lo que depende el panel de progreso. */
    fun observarCambios(uid: String): LiveData<Unit> = MediatorLiveData<Unit>().apply {
        addSource(dao.observarTotalMovimientos(uid)) { value = Unit }
        addSource(dao.observarTotalAsistencias(uid)) { value = Unit }
        addSource(dao.observarTotalSesiones(uid)) { value = Unit }
    }

    suspend fun xpTotal(uid: String): Int = dao.xpTotal(uid, TIPOS_SIN_XP)

    suspend fun conteoPorTipo(uid: String): Map<String, Int> =
        dao.conteoPorTipo(uid).associate { it.tipo to it.total }

    suspend fun conteoPorTipoDesde(uid: String, desde: String): Map<String, Int> =
        dao.conteoPorTipoDesde(uid, desde).associate { it.tipo to it.total }

    suspend fun xpPorDiaDesde(uid: String, desde: String): List<XpPorDia> =
        dao.xpPorDiaDesde(uid, TIPOS_SIN_XP, desde)

    suspend fun diasConTipos(uid: String, tipos: List<String>): List<String> = dao.diasConTipos(uid, tipos)

    suspend fun clavesDeTipo(uid: String, tipo: String): List<ClaveConFecha> = dao.clavesDeTipo(uid, tipo)

    // Misiones
    suspend fun misionesAsignadas(uid: String, periodo: String): List<String> = dao.misionesAsignadas(uid, periodo)

    suspend fun asignarMisiones(uid: String, periodo: String, ids: List<String>) =
        dao.asignarMisiones(ids.map { MisionAsignadaEntity(uid, periodo, it) })

    // Enfoque
    suspend fun sesionEnfoqueActiva(uid: String): SesionEnfoqueEntity? = dao.sesionEnfoqueActiva(uid)

    suspend fun sesionesEnfoqueCompletadas(uid: String): List<SesionEnfoqueEntity> = dao.sesionesEnfoqueCompletadas(uid)

    suspend fun insertarSesionEnfoque(sesion: SesionEnfoqueEntity) = dao.insertarSesionEnfoque(sesion)

    suspend fun completarSesionEnfoque(uid: String, inicioMillis: Long, millis: Long) =
        dao.completarSesionEnfoque(uid, inicioMillis, millis)

    suspend fun borrarSesionEnfoque(uid: String, inicioMillis: Long) = dao.borrarSesionEnfoque(uid, inicioMillis)

    // Asistencia
    suspend fun asistencias(uid: String): List<AsistenciaEntity> = dao.asistencias(uid)

    suspend fun asistencia(uid: String, asignaturaId: Int, fecha: String): AsistenciaEntity? =
        dao.asistencia(uid, asignaturaId, fecha)

    suspend fun insertarAsistencia(asistencia: AsistenciaEntity) = dao.insertarAsistencia(asistencia)

    // Inventario y avatar
    fun observarInventario(uid: String): LiveData<Set<String>> =
        dao.observarInventario(uid).map { lista -> lista.map { it.itemId }.toSet() }

    suspend fun posee(uid: String, itemId: String): Boolean = dao.contarEnInventario(uid, itemId) > 0

    suspend fun agregarAlInventario(uid: String, itemId: String, compradoMillis: Long) =
        dao.insertarEnInventario(InventarioEntity(uid, itemId, compradoMillis))

    /** Categoría → id del accesorio equipado. */
    fun observarEquipados(uid: String): LiveData<Map<String, String>> =
        dao.observarEquipados(uid).map { lista -> lista.associate { it.categoria to it.itemId } }

    suspend fun equipar(uid: String, categoria: String, itemId: String) =
        dao.equipar(AvatarEquipadoEntity(uid, categoria, itemId))

    suspend fun retirar(uid: String, categoria: String) = dao.retirar(uid, categoria)

    fun observarApariencia(uid: String): LiveData<AvatarAparienciaEntity?> = dao.observarApariencia(uid)

    suspend fun guardarApariencia(uid: String, indicePiel: Int, indicePelo: Int) =
        dao.guardarApariencia(AvatarAparienciaEntity(uid, indicePiel, indicePelo))
}
