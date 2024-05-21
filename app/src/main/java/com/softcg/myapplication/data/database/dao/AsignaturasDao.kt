package com.softcg.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.AsignaturaEntity
import com.softcg.myapplication.data.database.entities.EventoEntity

@Dao
interface AsignaturasDao {

    @Query("Select * From asignaturas_table")
    fun getAllAsignaturas():List<AsignaturaEntity>

    @Delete
    suspend fun deleteAsignatura(asignaturaEntity: AsignaturaEntity)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertAsignatura(asignatura: AsignaturaEntity)

}