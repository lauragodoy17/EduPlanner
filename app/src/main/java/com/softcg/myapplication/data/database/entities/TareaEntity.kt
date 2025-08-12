package com.softcg.myapplication.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tareas_table", indices = [Index(value = ["id"], unique = true)])
data class TareaEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")val id: Int? =null,
    @ColumnInfo(name="titulo")val titulo:String,
    @ColumnInfo(name = "descrip")val descrip:String,
    @ColumnInfo(name = "asignatura")val asignatura:String,
    @ColumnInfo(name= "fecha")val fecha:String,
    @ColumnInfo(name= "prioridad")val prioridad:Int
)