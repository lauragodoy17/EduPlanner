package com.softcg.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.softcg.myapplication.data.database.entities.EventoEntity
import com.softcg.myapplication.data.database.entities.TareaEntity

@Dao
interface EventosDao {

    @Query("SELECT * FROM eventos_table")
    fun getAllEventos():List<EventoEntity>

    @Delete
    suspend fun deleteEvento(eventoEntity: EventoEntity)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertEvento(evento: EventoEntity)

    @Update
    suspend fun updateEvento(evento: EventoEntity)

}