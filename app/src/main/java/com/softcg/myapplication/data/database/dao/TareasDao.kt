package com.softcg.myapplication.data.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.TareaEntity

@Dao
interface TareasDao {
    @Query("SELECT * FROM tareas_table ORDER BY id DESC")
    fun getAllTareas():List<TareaEntity>

    /** Tareas con la calificación que las completó (null si siguen pendientes). */
    @Query("SELECT t.*, c.valor AS calificacion FROM tareas_table t LEFT JOIN calificaciones_table c ON c.tarea_id = t.id ORDER BY t.id DESC")
    fun getAllTareasConCalificacion():List<TareaConCalificacion>

    @Delete
    suspend fun deleteTarea(tareaEntity: TareaEntity)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertAll(tareas:List<TareaEntity>)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertTarea(tarea:TareaEntity): Long

    /** Devuelve 0 si la tarea no existe o ya estaba entregada. */
    @Query("UPDATE tareas_table SET entregada_millis = :millis WHERE id = :id AND entregada_millis IS NULL")
    suspend fun marcarEntregada(id:Int, millis:Long): Int

}

data class TareaConCalificacion(
    @Embedded val tarea: TareaEntity,
    @ColumnInfo(name = "calificacion") val calificacion: Float?
)
