package com.softcg.myapplication.ui.Inicio.Models

import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion

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
    val entregadaMillis:Long? = null,
    // Tema del catálogo del modelo predictivo; null si no se eligió
    val tema:String? = null,
    val asignaturaId:Int? = null,
    // "HH:mm" del día de entrega; null = hasta las 23:59
    val horaEntrega:String? = null,
    // Escala de notas de su asignatura (para mostrar la calificación); la pone el repositorio al leer
    val escalaNota: EscalaCalificacion? = null
) {
    val completada: Boolean get() = calificacion != null || entregadaMillis != null

    /** La calificación como se muestra ("4,5", "A", "Aprobado"); null si no tiene. */
    val textoCalificacion: String? get() = calificacion?.let { EscalaCalificacion.texto(it, escalaNota) }
}
