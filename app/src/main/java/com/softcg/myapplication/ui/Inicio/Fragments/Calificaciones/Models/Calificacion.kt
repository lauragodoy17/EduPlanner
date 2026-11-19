package com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models

import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion

data class Calificacion (
    val id:Int?,
    val tipo:String,
    val valor:Float,
    val asignatura:String,
    val descripcion:String,
    // Tarea que esta calificación completó, si se registró al calificar una tarea
    val tareaId:Int? = null,
    val asignaturaId:Int? = null,
    // Escala con que se lee el valor (la de la nota o la de su asignatura); la pone el repositorio al leer
    val escala: EscalaCalificacion? = null
) {
    /** "4,5", "85 %", "A", "Aprobado". */
    val textoValor: String get() = EscalaCalificacion.texto(valor, escala)
}
