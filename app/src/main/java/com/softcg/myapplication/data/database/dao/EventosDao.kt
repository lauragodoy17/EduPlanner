package com.softcg.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.softcg.myapplication.data.database.entities.EventoEntity

@Dao
interface EventosDao {

    @Query("SELECT * FROM eventos_table WHERE uid = :uid")
    fun getAllEventos(uid: String):List<EventoEntity>

    @Query("SELECT * FROM eventos_table WHERE uid = :uid AND id = :id")
    suspend fun getEvento(uid: String, id: Int): EventoEntity?

    @Delete
    suspend fun deleteEvento(eventoEntity: EventoEntity)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertEvento(evento: EventoEntity): Long

    @Update
    suspend fun updateEvento(evento: EventoEntity)

    @Query("UPDATE eventos_table SET uid = :uid WHERE uid = ''")
    fun adoptarSinDueno(uid: String): Int
}
