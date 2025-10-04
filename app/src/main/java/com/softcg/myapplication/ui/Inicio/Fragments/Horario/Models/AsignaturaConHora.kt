package com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models

import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura

data class AsignaturaConHora(
    val asignatura: Asignatura,
    val horaInicio: String, // Formato "HH:mm"
    val horaFin: String,    // Formato "HH:mm"
    val colorIndex: Int = 0  // Índice para determinar el color pastel
) {
    // Constructor para crear desde Asignatura básica con hora estimada
    constructor(asignatura: Asignatura, colorIndex: Int = 0) : this(
        asignatura = asignatura,
        horaInicio = generateRandomStartTime(),
        horaFin = generateEndTime(generateRandomStartTime(), asignatura.duracion),
        colorIndex = colorIndex
    )

    companion object {
        private fun generateRandomStartTime(): String {
            val startHours = listOf("07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00")
            return startHours.random()
        }

        private fun generateEndTime(startTime: String, duracion: Int): String {
            val (hour, minute) = startTime.split(":").map { it.toInt() }
            val totalMinutes = hour * 60 + minute + (duracion * 60)
            val endHour = (totalMinutes / 60) % 24
            val endMinute = totalMinutes % 60
            return String.format("%02d:%02d", endHour, endMinute)
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