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

    /** Escala de notas: el tipo de calificación y su rango, juntos. */
    @Query("Update asignaturas_table Set tipo_calificacion = :tipo, escala_min = :escalaMin, escala_max = :escalaMax Where id = :id And uid = :uid")
    suspend fun guardarEscala(uid: String, id: Int, tipo: String, escalaMin: Float, escalaMax: Float): Int

    @Query("Update asignaturas_table Set area_modelo = :area, escala_min = :escalaMin, escala_max = :escalaMax Where id = :id And uid = :uid")
    suspend fun guardarDatosModelo(uid: String, id: Int, area: String?, escalaMin: Float?, escalaMax: Float?): Int

    /** Edita los datos del formulario; el área, la escala y la fecha de registro no cambian. */
    @Query("Update asignaturas_table Set nombre = :nombre, tutor = :tutor, Duracion = :duracion, hora = :hora, lunes = :lunes, martes = :martes, miercoles = :miercoles, jueves = :jueves, viernes = :viernes, sabado = :sabado, domingo = :domingo, tipo_calificacion = :tipoCalificacion Where id = :id And uid = :uid")
    suspend fun actualizar(
        uid: String, id: Int, nombre: String, tutor: String, duracion: Int, hora: String,
        lunes: Boolean, martes: Boolean, miercoles: Boolean, jueves: Boolean, viernes: Boolean, sabado: Boolean, domingo: Boolean,
        tipoCalificacion: String
    ): Int

    @Query("Update asignaturas_table Set uid = :uid Where uid = ''")
    fun adoptarSinDueno(uid: String): Int
}
