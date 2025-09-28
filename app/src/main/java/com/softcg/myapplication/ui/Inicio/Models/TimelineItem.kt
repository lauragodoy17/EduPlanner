package com.softcg.myapplication.ui.Inicio.Models

import java.util.*

data class TimelineItem(
    val id: Int?,
    val titulo: String,
    val descrip: String,
    val fecha: String,
    val prioridad: Int,
    val type: TimelineItemType,
    val asignatura: String? = null // Solo para tareas
) {
    fun getDateForSorting(): Date {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(fecha) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}

enum class TimelineItemType {
    TAREA,
    EVENTO
}

// Extension functions para convertir desde modelos existentes
fun Tarea.toTimelineItem(): TimelineItem {
    return TimelineItem(
        id = this.id,
        titulo = this.titulo,
        descrip = this.descrip,
        fecha = this.fecha,
        prioridad = this.prioridad,
        type = TimelineItemType.TAREA,
        asignatura = this.asignatura
    )
}

fun Evento.toTimelineItem(): TimelineItem {
    return TimelineItem(
        id = this.id,
        titulo = this.titulo,
        descrip = this.descrip,
        fecha = this.fecha,
        prioridad = this.prioridad,
        type = TimelineItemType.EVENTO,
        asignatura = null
    )
}