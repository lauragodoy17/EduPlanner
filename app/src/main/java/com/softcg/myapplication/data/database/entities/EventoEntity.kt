package com.softcg.myapplication.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos_table")
data class EventoEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")val id: Int?=null,
    @ColumnInfo(name="titulo")val titulo:String,
    @ColumnInfo(name = "descrip")val descrip:String,
    @ColumnInfo(name= "fecha")val fecha:String,
    @ColumnInfo(name= "prioridad")val prioridad:Int,
    @ColumnInfo(name= "horaInicio")val horaInicio:String = "",
    @ColumnInfo(name= "horaFin")val horaFin:String = "",
    @ColumnInfo(name= "imagenUri")val imagenUri:String? = null
)