package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models

import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion

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
    // Rango de la escala de notas (las asignaturas nuevas lo guardan siempre, también con las escalas predefinidas)
    val escalaMin:Float? = null,
    val escalaMax:Float? = null
) {
    /** Escala de notas; null en asignaturas antiguas que aún no la tienen. */
    val escala: EscalaCalificacion? get() = EscalaCalificacion.de(this)
}
