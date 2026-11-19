package com.softcg.myapplication.ui.Inicio.Dialogs

import android.view.View
import android.widget.TextView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.estiloPrioridad
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.core.ex.pintarPrioridad
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Etiqueta del estado de la tarea: TAREA, ENTREGADA o COMPLETADA (calificada). */
fun TextView.pintarEstadoTarea(tarea: Tarea) = when {
    tarea.calificacion != null -> pintarEtiqueta("COMPLETADA", R.color.estado_exito, R.color.estado_exito_suave)
    tarea.entregadaMillis != null -> pintarEtiqueta("ENTREGADA", R.color.estado_exito, R.color.estado_exito_suave)
    else -> pintarEtiqueta("TAREA", R.color.acento, R.color.marca_superficie)
}

/** Pinta `include_info_tarea` dentro de [raiz]: lo que no tiene valor (descripción, calificación) se oculta. */
fun pintarInfoTarea(raiz: View, tarea: Tarea, hoy: LocalDate = LocalDate.now()) {
    raiz.findViewById<TextView>(R.id.tareaTitulo).text = tarea.titulo.trim()

    val descripcion = raiz.findViewById<TextView>(R.id.tareaDescripcion)
    descripcion.text = tarea.descrip.trim()
    descripcion.visibility = if (tarea.descrip.isNotBlank()) View.VISIBLE else View.GONE
    raiz.findViewById<View>(R.id.labelDescripcion).visibility = descripcion.visibility

    raiz.findViewById<TextView>(R.id.tareaAsignatura).text = tarea.asignatura.ifBlank { "Sin asignatura" }
    raiz.findViewById<TextView>(R.id.tareaFecha).text = textoFechaTarea(tarea, hoy)

    val nota = tarea.calificacion
    raiz.findViewById<TextView>(R.id.tareaCalificacion).apply {
        text = nota?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }.orEmpty()
        visibility = if (nota != null) View.VISIBLE else View.GONE
    }
    raiz.findViewById<View>(R.id.labelCalificacion).visibility = if (nota != null) View.VISIBLE else View.GONE

    val punto = raiz.findViewById<View>(R.id.prioridadIndicador)
    val nombre = raiz.findViewById<TextView>(R.id.prioridadTexto)
    pintarPrioridad(tarea.prioridad, punto, nombre)
    raiz.findViewById<View>(R.id.labelPrioridad).visibility = nombre.visibility
    estiloPrioridad(tarea.prioridad)?.let { nombre.setTextColor(raiz.context.getColor(it.color)) }
}

/** "20/11/2026 · faltan 8 días"; sin el plazo si la tarea ya está hecha. */
fun textoFechaTarea(tarea: Tarea, hoy: LocalDate): String {
    val fecha = runCatching { LocalDate.parse(tarea.fecha) }.getOrNull() ?: return tarea.fecha
    val dia = fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    val texto = tarea.horaEntrega?.takeIf { it.isNotBlank() }?.let { "$dia · hasta las $it" } ?: dia
    if (tarea.completada) return texto
    val plazo = when (val dias = ChronoUnit.DAYS.between(hoy, fecha)) {
        0L -> "hoy"
        1L -> "mañana"
        -1L -> "venció ayer"
        in 2L..Long.MAX_VALUE -> "faltan $dias días"
        else -> "venció hace ${-dias} días"
    }
    return "$texto · $plazo"
}
