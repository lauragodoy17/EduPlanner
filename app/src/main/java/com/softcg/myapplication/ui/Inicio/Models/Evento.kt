package com.softcg.myapplication.ui.Inicio.Models

data class Evento (
    val id:Int?,
    val titulo:String,
    val descrip:String,
    val fecha:String,
    val prioridad:Int,
    val horaInicio:String = "",
    val horaFin:String = "",
    val imagenUri:String? = null
)