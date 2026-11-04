package com.softcg.myapplication.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "calificaciones_table", indices = [Index(value = ["tarea_id"], unique = true)])
data class CalificacionEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")val id: Int?=null,
    @ColumnInfo(name="tipo")val tipo:String,
    @ColumnInfo(name = "valor")val valor:Float,
    @ColumnInfo(name = "asignatura")val asignatura:String,
    @ColumnInfo(name = "descripcion")val descripcion:String,
    // Tarea que quedó completada con esta calificación (null si se registró desde Calificaciones)
    @ColumnInfo(name = "tarea_id")val tareaId:Int? = null
    )
