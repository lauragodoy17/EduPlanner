package com.softcg.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.CalificacionEntity

@Dao
interface CalificacionesDao {
    @Query("Select * From calificaciones_table Where uid = :uid")
    fun getAllCalificaciones(uid: String):List<CalificacionEntity>

    /** Notas de una asignatura: por id, o por nombre las registradas antes de enlazarse por id. */
    @Query("Select * From calificaciones_table Where uid = :uid And (asignatura_id = :asignaturaId Or (asignatura_id Is Null And asignatura = :nombre))")
    suspend fun deAsignatura(uid: String, asignaturaId: Int, nombre: String): List<CalificacionEntity>

    @Query("Select COUNT(*) From calificaciones_table Where tarea_id = :tareaId")
    suspend fun contarDeTarea(tareaId:Int):Int

    @Delete
    suspend fun deleteCalificacion(calificacionEntity: CalificacionEntity)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertCalificacion(calificacionEntity: CalificacionEntity): Long

    @Query("Update calificaciones_table Set uid = :uid Where uid = ''")
    fun adoptarSinDueno(uid: String): Int

    @Query("Update calificaciones_table Set asignatura_id = :asignaturaId Where uid = :uid And asignatura_id Is Null And asignatura = :nombre")
    suspend fun enlazarAsignatura(uid: String, asignaturaId: Int, nombre: String): Int
}
