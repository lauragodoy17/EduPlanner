package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models

data class Asignatura(
    val id:Int?,
    val nombre:String,
    val tutor:String,
    val duracion:Int,
    val hora:String,
    val horario:List<Boolean>,
    val tipoCalificacion:String = "",
    val fechaRegistro:Long = 0L,
    // Área del catálogo del modelo predictivo (subject_name); null hasta que el estudiante la confirme
    val areaModelo:String? = null,
    // Rango de notas cuando el tipo de calificación no lo indica
    val escalaMin:Float? = null,
    val escalaMax:Float? = null
)
