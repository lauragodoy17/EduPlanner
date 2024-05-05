package com.softcg.myapplication.ui.tarea.model

import androidx.room.ColumnInfo

data class Tarea (
    val id:Int?,
    val titulo:String,
    val descrip:String,
    val asignatura:String,
    val fecha:String
)