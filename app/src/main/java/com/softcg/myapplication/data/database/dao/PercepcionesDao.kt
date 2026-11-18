package com.softcg.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.PercepcionEntity

@Dao
interface PercepcionesDao {

    /** Misma pregunta (uid, tipo, origen) = se reemplaza la respuesta anterior. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(percepcion: PercepcionEntity)

    @Query("SELECT * FROM percepciones WHERE uid = :uid AND asignatura_id = :asignaturaId ORDER BY fecha_millis DESC")
    suspend fun deAsignatura(uid: String, asignaturaId: Int): List<PercepcionEntity>

    @Query("SELECT * FROM percepciones WHERE uid = :uid ORDER BY fecha_millis DESC")
    suspend fun todas(uid: String): List<PercepcionEntity>

    @Query("SELECT COUNT(*) FROM percepciones WHERE uid = :uid AND tipo = :tipo AND origen = :origen")
    suspend fun contar(uid: String, tipo: String, origen: String): Int
}
