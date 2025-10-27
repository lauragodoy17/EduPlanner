package com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models

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

        // Colores pasteles predefinidos para las tarjetas
        val COLORES_PASTELES = listOf(
            "#FFE4E1", // Rosa pastel
            "#E1F5FE", // Azul pastel
            "#F3E5F5", // Púrpura pastel
            "#E8F5E8", // Verde pastel
            "#FFF3E0", // Naranja pastel
            "#F1F8E9", // Verde lima pastel
            "#FCE4EC", // Rosa magenta pastel
            "#E3F2FD", // Azul cielo pastel
            "#FFF8E1", // Amarillo pastel
            "#F9FBE7"  // Verde claro pastel
        )

        fun getColorPastel(index: Int): String {
            return COLORES_PASTELES[index % COLORES_PASTELES.size]
        }
    }
}