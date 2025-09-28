package com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models

data class AgendaItem (
    val id:Int?,
    val titulo:String,
    val descrip:String,
    val asignatura:String?,
    val fecha:String,
    val prioridad:Int = 1
)