package com.softcg.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.CalificacionEntity

@Dao
interface CalificacionesDao {
    @Query("Select * From calificaciones_table")
    fun getAllCalificaciones():List<CalificacionEntity>

    @Delete
    suspend fun deleteCalificacion(calificacionEntity: CalificacionEntity)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertCalificacion(calificacionEntity: CalificacionEntity)
}