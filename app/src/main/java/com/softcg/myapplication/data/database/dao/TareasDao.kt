package com.softcg.myapplication.data.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.TareaEntity

/** Todas las consultas van filtradas por el uid del usuario con sesión. */
@Dao
interface TareasDao {
    @Query("SELECT * FROM tareas_table WHERE uid = :uid ORDER BY id DESC")
    fun getAllTareas(uid: String):List<TareaEntity>

    /** Tareas con la calificación que las completó (null si siguen pendientes). */
    @Query("SELECT t.*, c.valor AS calificacion FROM tareas_table t LEFT JOIN calificaciones_table c ON c.tarea_id = t.id WHERE t.uid = :uid ORDER BY t.id DESC")
    fun getAllTareasConCalificacion(uid: String):List<TareaConCalificacion>

    @Query("SELECT t.*, c.valor AS calificacion FROM tareas_table t LEFT JOIN calificaciones_table c ON c.tarea_id = t.id WHERE t.uid = :uid AND t.id = :id")
    suspend fun getTarea(uid: String, id: Int): TareaConCalificacion?

    @Delete
    suspend fun deleteTarea(tareaEntity: TareaEntity)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertAll(tareas:List<TareaEntity>)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertTarea(tarea:TareaEntity): Long

    /** Devuelve 0 si la tarea no existe o ya estaba entregada. */
    @Query("UPDATE tareas_table SET entregada_millis = :millis WHERE id = :id AND uid = :uid AND entregada_millis IS NULL")
    suspend fun marcarEntregada(uid: String, id:Int, millis:Long): Int

    /** Filas anteriores a la versión 16, sin dueño. */
    /** Edita lo que el estudiante escribió; el estado (entregada, calificada) no cambia. */
    @Query("UPDATE tareas_table SET titulo = :titulo, descrip = :descrip, asignatura = :asignatura, asignatura_id = :asignaturaId, fecha = :fecha, prioridad = :prioridad, hora_entrega = :horaEntrega WHERE id = :id AND uid = :uid")
    suspend fun actualizar(uid: String, id: Int, titulo: String, descrip: String, asignatura: String, asignaturaId: Int?, fecha: String, prioridad: Int, horaEntrega: String?): Int

    /** Al renombrar una asignatura, sus tareas (por id o, las antiguas, por nombre) pasan al nombre nuevo. */
    @Query("UPDATE tareas_table SET asignatura = :nuevo, asignatura_id = :asignaturaId WHERE uid = :uid AND (asignatura_id = :asignaturaId OR (asignatura_id IS NULL AND asignatura = :viejo))")
    suspend fun renombrarAsignatura(uid: String, asignaturaId: Int, viejo: String, nuevo: String): Int

    @Query("UPDATE tareas_table SET uid = :uid WHERE uid = ''")
    fun adoptarSinDueno(uid: String): Int

    /** Enlaza por id las tareas cuya asignatura se escribió con este nombre. */
    @Query("UPDATE tareas_table SET asignatura_id = :asignaturaId WHERE uid = :uid AND asignatura_id IS NULL AND asignatura = :nombre")
    suspend fun enlazarAsignatura(uid: String, asignaturaId: Int, nombre: String): Int
}

data class TareaConCalificacion(
    @Embedded val tarea: TareaEntity,
    @ColumnInfo(name = "calificacion") val calificacion: Float?
)
