package com.softcg.myapplication.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historial de EduCoins. El saldo de un usuario es siempre la suma de sus movimientos;
 * no se guarda un saldo aparte. La clave es única por usuario y evita recompensas o cobros dobles.
 */
@Entity(
    tableName = "educoins_movimientos",
    indices = [Index(value = ["uid", "clave"], unique = true)]
)
data class MovimientoEduCoinsEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "clave") val clave: String,
    @ColumnInfo(name = "tipo") val tipo: String,
    @ColumnInfo(name = "cantidad") val cantidad: Int,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "fecha_millis") val fechaMillis: Long,
    @ColumnInfo(name = "dia") val dia: String
)

/** Respuesta a la pregunta de asistencia de una clase concreta (asignatura + fecha). */
@Entity(tableName = "asistencias", primaryKeys = ["uid", "asignatura_id", "fecha"])
data class AsistenciaEntity(
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "asignatura_id") val asignaturaId: Int,
    @ColumnInfo(name = "fecha") val fecha: String,
    @ColumnInfo(name = "asistio") val asistio: Boolean,
    @ColumnInfo(name = "inicio_millis") val inicioMillis: Long,
    @ColumnInfo(name = "fin_millis") val finMillis: Long,
    @ColumnInfo(name = "confirmada_millis") val confirmadaMillis: Long
)

@Entity(tableName = "inventario", primaryKeys = ["uid", "item_id"])
data class InventarioEntity(
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "comprado_millis") val compradoMillis: Long
)

/** Un accesorio equipado por categoría y usuario. */
@Entity(tableName = "avatar_equipado", primaryKeys = ["uid", "categoria"])
data class AvatarEquipadoEntity(
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "categoria") val categoria: String,
    @ColumnInfo(name = "item_id") val itemId: String
)

/** Tono de piel y pelo elegidos por el usuario; sin fila se usan los derivados del uid. */
@Entity(tableName = "avatar_apariencia")
data class AvatarAparienciaEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "indice_piel") val indicePiel: Int,
    @ColumnInfo(name = "indice_pelo") val indicePelo: Int
)

/** Misiones asignadas a un periodo ("D:<fecha>" o "S:<lunes>"): el conjunto no cambia durante ese periodo. */
@Entity(tableName = "misiones_asignadas", primaryKeys = ["uid", "periodo", "mision_id"])
data class MisionAsignadaEntity(
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "periodo") val periodo: String,
    @ColumnInfo(name = "mision_id") val misionId: String
)

/** Sesión de enfoque; con [completadaMillis] null sigue en curso. Abandonarla borra la fila. */
@Entity(tableName = "sesiones_enfoque", primaryKeys = ["uid", "inicio_millis"])
data class SesionEnfoqueEntity(
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "inicio_millis") val inicioMillis: Long,
    @ColumnInfo(name = "minutos") val minutos: Int,
    @ColumnInfo(name = "asignatura") val asignatura: String,
    @ColumnInfo(name = "completada_millis") val completadaMillis: Long?
)

/** Filas de agregación de [com.softcg.myapplication.data.database.dao.GamificacionDao]. */
data class ConteoPorTipo(
    @ColumnInfo(name = "tipo") val tipo: String,
    @ColumnInfo(name = "total") val total: Int
)

data class XpPorDia(
    @ColumnInfo(name = "dia") val dia: String,
    @ColumnInfo(name = "total") val total: Int
)

data class ClaveConFecha(
    @ColumnInfo(name = "clave") val clave: String,
    @ColumnInfo(name = "fecha_millis") val fechaMillis: Long
)
