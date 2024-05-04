package com.softcg.myapplication.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.TareaEntity
import kotlin.coroutines.Continuation

@Dao
interface TareasDao {
    @Query("SELECT * FROM tareas_table ORDER BY id DESC")
    fun getAllTareas():List<TareaEntity>

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertAll(tareas:List<TareaEntity>)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertTarea(tarea:TareaEntity)

}