package com.softcg.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.softcg.myapplication.data.database.entities.AsignaturaEntity

@Dao
interface AsignaturasDao {

    @Query("Select * From asignaturas_table Where uid = :uid")
    fun getAllAsignaturas(uid: String):List<AsignaturaEntity>

    @Query("Select * From asignaturas_table Where id = :id And uid = :uid")
    suspend fun getAsignatura(uid: String, id:Int):AsignaturaEntity?

    /** La primera asignatura con ese nombre exacto (las pantallas antiguas guardan el nombre, no el id). */
    @Query("Select id From asignaturas_table Where uid = :uid And nombre = :nombre Order By id Limit 1")
    fun idPorNombre(uid: String, nombre: String): Int?

    @Delete
    suspend fun deleteAsignatura(asignaturaEntity: AsignaturaEntity)

    @Insert(onConflict= OnConflictStrategy.REPLACE)
    suspend fun insertAsignatura(asignatura: AsignaturaEntity): Long

    @Query("Update asignaturas_table Set area_modelo = :area, escala_min = :escalaMin, escala_max = :escalaMax Where id = :id And uid = :uid")
    suspend fun guardarDatosModelo(uid: String, id: Int, area: String?, escalaMin: Float?, escalaMax: Float?): Int

    @Query("Update asignaturas_table Set uid = :uid Where uid = ''")
    fun adoptarSinDueno(uid: String): Int
}
