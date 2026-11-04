package com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models

data class Calificacion (
    val id:Int?,
    val tipo:String,
    val valor:Float,
    val asignatura:String,
    val descripcion:String,
    // Tarea que esta calificación completó, si se registró al calificar una tarea
    val tareaId:Int? = null
)
