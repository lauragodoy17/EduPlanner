package com.softcg.myapplication.ui.Inicio.Models

data class Tarea (
    val id:Int?,
    val titulo:String,
    val descrip:String,
    val asignatura:String,
    val fecha:String,
    val prioridad:Int,
    // Calificación registrada para la tarea; una tarea calificada se considera completada
    val calificacion:Float? = null,
    // Cuándo se marcó como entregada: la nota suele llegar después de la entrega
    val entregadaMillis:Long? = null
) {
    val completada: Boolean get() = calificacion != null || entregadaMillis != null
}
