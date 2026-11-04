package com.softcg.myapplication.domain.gamificacion

import java.time.LocalDate

data class SituacionEstudiante(
    val nivel: ProgresoNivel,
    val racha: Racha,
    val porReclamar: Int,
    val entregasHoy: Int,
    val entregasManana: Int,
    val hora: Int,
    val dia: LocalDate
)

/** Un mensaje útil según lo que pasa ahora, no una frase motivacional cualquiera. Gana lo más urgente. */
object MensajesMotivacion {

    private const val HORA_AVISO_RACHA = 18
    private const val XP_CERCA_DE_SUBIR = 25

    private val generales = listOf(
        "Lo que anotas hoy es lo que no te sorprende mañana.",
        "Una sesión corta hoy vale más que una maratón la noche antes.",
        "Empieza por lo más pequeño: arrancar es lo difícil.",
        "Revisa tu semana: cinco minutos de plan ahorran horas de afán.",
        "La constancia le gana al talento en parciales."
    )

    fun para(s: SituacionEstudiante): String {
        val siguiente = s.nivel.siguiente
        return when {
            s.porReclamar > 0 ->
                if (s.porReclamar == 1) "Tienes 1 recompensa lista para reclamar."
                else "Tienes ${s.porReclamar} recompensas listas para reclamar."
            s.racha.enRiesgo && s.hora >= HORA_AVISO_RACHA ->
                "Tu racha de ${s.racha.actual} días termina hoy. Con una actividad la mantienes."
            s.entregasHoy > 0 ->
                if (s.entregasHoy == 1) "Hoy vence 1 tarea: entrégala a tiempo y gana el bono."
                else "Hoy vencen ${s.entregasHoy} tareas: entrégalas a tiempo y gana el bono."
            s.entregasManana > 0 ->
                if (s.entregasManana == 1) "Mañana vence 1 tarea. Adelántala hoy y evita el afán."
                else "Mañana vencen ${s.entregasManana} tareas. Adelanta una hoy y evita el afán."
            siguiente != null && s.nivel.xpRestante <= XP_CERCA_DE_SUBIR ->
                "Te faltan ${s.nivel.xpRestante} XP para ser ${siguiente.nombre}."
            s.racha.actual >= 7 ->
                "${s.racha.actual} días seguidos. Ya no es suerte, es hábito."
            else -> generales[(s.dia.toEpochDay() % generales.size).toInt()]
        }
    }
}
