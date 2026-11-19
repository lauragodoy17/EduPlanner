package com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models

data class AgendaItem (
    val id:Int?,
    val titulo:String,
    val descrip:String,
    /** null = evento. */
    val asignatura:String?,
    val fecha:String,
    val prioridad:Int = 1,
    /** Tarea ya entregada (o calificada). */
    val entregada: Boolean = false,
    /** La calificación en su escala ("4,5", "A"); null si no tiene. */
    val textoCalificacion: String? = null
)

/** Qué muestra la agenda. Por defecto, lo que falta por hacer: tareas sin entregar y eventos que no han pasado. */
enum class FiltroAgenda(val texto: String) {
    SIN_ENTREGAR("Sin entregar"),
    SOLO_TAREAS("Solo tareas"),
    SOLO_EVENTOS("Solo eventos"),
    ENTREGADOS("Entregados");

    /** "3 pendientes", "1 tarea sin entregar"... */
    fun resumen(cantidad: Int): String = when (this) {
        SIN_ENTREGAR -> if (cantidad == 1) "1 pendiente" else "$cantidad pendientes"
        SOLO_TAREAS -> if (cantidad == 1) "1 tarea sin entregar" else "$cantidad tareas sin entregar"
        SOLO_EVENTOS -> if (cantidad == 1) "1 evento próximo" else "$cantidad eventos próximos"
        ENTREGADOS -> if (cantidad == 1) "1 tarea entregada" else "$cantidad tareas entregadas"
    }

    /** Título y detalle de la lista vacía. */
    val vacio: Pair<String, String>
        get() = when (this) {
            SIN_ENTREGAR -> "Tu agenda está libre" to "Agrega una tarea o un evento con el botón + para verlos aquí"
            SOLO_TAREAS -> "No tienes tareas pendientes" to "Las tareas que aún no entregas aparecerán aquí"
            SOLO_EVENTOS -> "No tienes eventos próximos" to "Agrega un evento con el botón + para verlo aquí"
            ENTREGADOS -> "Aún no has entregado tareas" to "Cuando entregues o califiques una tarea, aparecerá aquí"
        }
}
