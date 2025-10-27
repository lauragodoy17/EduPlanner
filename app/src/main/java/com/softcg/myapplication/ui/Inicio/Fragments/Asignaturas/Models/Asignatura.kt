package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models

data class Asignatura(
    val id:Int?,
    val nombre:String,
    val tutor:String,
    val duracion:Int,
    val hora:String,
    val horario:List<Boolean>
)
