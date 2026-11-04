package com.softcg.myapplication.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.AsistenciaEntity
import com.softcg.myapplication.data.database.entities.AvatarAparienciaEntity
import com.softcg.myapplication.data.database.entities.AvatarEquipadoEntity
import com.softcg.myapplication.data.database.entities.ClaveConFecha
import com.softcg.myapplication.data.database.entities.ConteoPorTipo
import com.softcg.myapplication.data.database.entities.InventarioEntity
import com.softcg.myapplication.data.database.entities.MisionAsignadaEntity
import com.softcg.myapplication.data.database.entities.MovimientoEduCoinsEntity
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.database.entities.XpPorDia

@Dao
interface GamificacionDao {

    // EduCoins
    @Query("SELECT COALESCE(SUM(cantidad), 0) FROM educoins_movimientos WHERE uid = :uid")
    fun observarSaldo(uid: String): LiveData<Int>

    @Query("SELECT COALESCE(SUM(cantidad), 0) FROM educoins_movimientos WHERE uid = :uid")
    suspend fun saldo(uid: String): Int

    @Query("SELECT * FROM educoins_movimientos WHERE uid = :uid ORDER BY fecha_millis DESC, id DESC")
    fun observarMovimientos(uid: String): LiveData<List<MovimientoEduCoinsEntity>>

    @Query("SELECT COUNT(*) FROM educoins_movimientos WHERE uid = :uid AND clave = :clave")
    suspend fun contarClave(uid: String, clave: String): Int

    @Query("SELECT COUNT(*) FROM educoins_movimientos WHERE uid = :uid AND tipo = :tipo AND dia = :dia")
    suspend fun contarPorTipoYDia(uid: String, tipo: String, dia: String): Int

    /** Devuelve -1 si ya existía un movimiento con la misma clave para ese usuario. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarMovimiento(movimiento: MovimientoEduCoinsEntity): Long

    // Asistencia
    @Query("SELECT * FROM asistencias WHERE uid = :uid")
    suspend fun asistencias(uid: String): List<AsistenciaEntity>

    @Query("SELECT * FROM asistencias WHERE uid = :uid AND asignatura_id = :asignaturaId AND fecha = :fecha")
    suspend fun asistencia(uid: String, asignaturaId: Int, fecha: String): AsistenciaEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarAsistencia(asistencia: AsistenciaEntity)

    // Inventario y avatar
    @Query("SELECT * FROM inventario WHERE uid = :uid ORDER BY comprado_millis")
    fun observarInventario(uid: String): LiveData<List<InventarioEntity>>

    @Query("SELECT COUNT(*) FROM inventario WHERE uid = :uid AND item_id = :itemId")
    suspend fun contarEnInventario(uid: String, itemId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarEnInventario(item: InventarioEntity)

    @Query("SELECT * FROM avatar_equipado WHERE uid = :uid")
    fun observarEquipados(uid: String): LiveData<List<AvatarEquipadoEntity>>

    @Query("SELECT * FROM avatar_equipado WHERE uid = :uid")
    suspend fun equipados(uid: String): List<AvatarEquipadoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun equipar(equipado: AvatarEquipadoEntity)

    @Query("DELETE FROM avatar_equipado WHERE uid = :uid AND categoria = :categoria")
    suspend fun retirar(uid: String, categoria: String)

    @Query("SELECT * FROM avatar_apariencia WHERE uid = :uid")
    fun observarApariencia(uid: String): LiveData<AvatarAparienciaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarApariencia(apariencia: AvatarAparienciaEntity)

    // Progreso:se deriva de los movimientos, igual que el saldo
    @Query("SELECT COUNT(*) FROM educoins_movimientos WHERE uid = :uid")
    fun observarTotalMovimientos(uid: String): LiveData<Int>

    @Query("SELECT COALESCE(SUM(cantidad), 0) FROM educoins_movimientos WHERE uid = :uid AND cantidad > 0 AND tipo NOT IN (:sinXp)")
    suspend fun xpTotal(uid: String, sinXp: List<String>): Int

    @Query("SELECT tipo, COUNT(*) AS total FROM educoins_movimientos WHERE uid = :uid GROUP BY tipo")
    suspend fun conteoPorTipo(uid: String): List<ConteoPorTipo>

    /** [desde] en formato yyyy-MM-dd: como texto se ordena igual que como fecha. */
    @Query("SELECT tipo, COUNT(*) AS total FROM educoins_movimientos WHERE uid = :uid AND dia >= :desde GROUP BY tipo")
    suspend fun conteoPorTipoDesde(uid: String, desde: String): List<ConteoPorTipo>

    @Query("SELECT dia, SUM(cantidad) AS total FROM educoins_movimientos WHERE uid = :uid AND cantidad > 0 AND tipo NOT IN (:sinXp) AND dia >= :desde GROUP BY dia")
    suspend fun xpPorDiaDesde(uid: String, sinXp: List<String>, desde: String): List<XpPorDia>

    @Query("SELECT DISTINCT dia FROM educoins_movimientos WHERE uid = :uid AND tipo IN (:tipos)")
    suspend fun diasConTipos(uid: String, tipos: List<String>): List<String>

    @Query("SELECT clave, fecha_millis FROM educoins_movimientos WHERE uid = :uid AND tipo = :tipo")
    suspend fun clavesDeTipo(uid: String, tipo: String): List<ClaveConFecha>

    // Misiones
    @Query("SELECT mision_id FROM misiones_asignadas WHERE uid = :uid AND periodo = :periodo")
    suspend fun misionesAsignadas(uid: String, periodo: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun asignarMisiones(misiones: List<MisionAsignadaEntity>)

    // Enfoque
    @Query("SELECT * FROM sesiones_enfoque WHERE uid = :uid AND completada_millis IS NULL LIMIT 1")
    suspend fun sesionEnfoqueActiva(uid: String): SesionEnfoqueEntity?

    @Query("SELECT * FROM sesiones_enfoque WHERE uid = :uid AND completada_millis IS NOT NULL")
    suspend fun sesionesEnfoqueCompletadas(uid: String): List<SesionEnfoqueEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarSesionEnfoque(sesion: SesionEnfoqueEntity)

    @Query("UPDATE sesiones_enfoque SET completada_millis = :millis WHERE uid = :uid AND inicio_millis = :inicioMillis")
    suspend fun completarSesionEnfoque(uid: String, inicioMillis: Long, millis: Long)

    @Query("DELETE FROM sesiones_enfoque WHERE uid = :uid AND inicio_millis = :inicioMillis")
    suspend fun borrarSesionEnfoque(uid: String, inicioMillis: Long)

    @Query("SELECT COUNT(*) FROM sesiones_enfoque WHERE uid = :uid")
    fun observarTotalSesiones(uid: String): LiveData<Int>

    @Query("SELECT COUNT(*) FROM asistencias WHERE uid = :uid")
    fun observarTotalAsistencias(uid: String): LiveData<Int>
}
