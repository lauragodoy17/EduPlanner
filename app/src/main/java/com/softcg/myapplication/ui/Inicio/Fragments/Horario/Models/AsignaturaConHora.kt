package com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models

import com.softcg.myapplication.domain.gamificacion.AsistenciaReglas
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura

data class AsignaturaConHora(
    val asignatura: Asignatura,
    val horaInicio: String, // Formato "HH:mm"
    val horaFin: String,    // Formato "HH:mm"
    val colorIndex: Int = 0  // Índice para determinar el color pastel
) {
    // Constructor para crear desde Asignatura básica usando la hora de la base de datos
    constructor(asignatura: Asignatura, colorIndex: Int = 0) : this(
        asignatura = asignatura,
        horaInicio = asignatura.hora.trim(),
        horaFin = generateEndTime(asignatura.hora.trim(), asignatura.duracion),
        colorIndex = colorIndex
    )

    companion object {
        /** Las asignaturas antiguas guardaron la hora como "14: 0"; en pantalla siempre va como 14:00. */
        fun horaLegible(hora: String): String =
            AsistenciaReglas.parsearHora(hora)?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: hora.trim()

        private fun generateEndTime(startTime: String, duracion: Int): String {
            try {
                val cleanTime = startTime.trim()
                val parts = cleanTime.split(":")
                if (parts.size < 2) {
                    android.util.Log.e("AsignaturaConHora", "Invalid time format: '$startTime'")
                    return "00:00"
                }

                val hour = parts[0].trim().toInt()
                val minute = parts[1].trim().toInt()
                val totalMinutes = hour * 60 + minute + (duracion * 60)
                val endHour = (totalMinutes / 60) % 24
                val endMinute = totalMinutes % 60
                return String.format("%02d:%02d", endHour, endMinute)
            } catch (e: Exception) {
                android.util.Log.e("AsignaturaConHora", "Error generating end time from '$startTime'", e)
                return "00:00"
            }
        }

        // Identifican a cada asignatura; bastante saturados para verse como barra sobre fondo blanco
        val COLORES_ASIGNATURA = listOf(
            "#9C27B0",
            "#2F80C2",
            "#E07B39",
            "#1E9E8F",
            "#D9487D",
            "#5E4BB8",
            "#4C9F50",
            "#D49B00",
            "#7F8FA6",
            "#B4764A"
        )

        fun getColorAsignatura(index: Int): String {
            return COLORES_ASIGNATURA[index % COLORES_ASIGNATURA.size]
        }
    }
}