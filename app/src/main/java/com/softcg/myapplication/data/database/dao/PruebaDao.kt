package com.softcg.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.TareaEntity

@Dao
interface PruebaDao {

    @Query("SELECT * FROM tareas_table")
    suspend fun getAllPrueba():List<TareaEntity>
}