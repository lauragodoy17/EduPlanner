package com.softcg.myapplication.domain.gamificacion

import java.time.LocalDate

data class EstadisticasProgreso(
    val xpTotal: Int,
    val tareasTerminadas: Int,
    /** null mientras no haya datos: un 0 % sin datos desmotiva y no significa nada. */
    val porcentajeATiempo: Int?,
    val porcentajeAsistencia: Int?,
    val minutosEnfoqueTotal: Int,
    val minutosEnfoqueSemana: Int,
    val mejorRacha: Int,
    val xpSemana: Int,
    val xpSemanaAnterior: Int,
    /** XP de los últimos 7 días, del más antiguo a hoy. */
    val ultimos7Dias: List<Pair<LocalDate, Int>>,
    /** XP de los días con actividad en las últimas semanas. */
    val actividad: Map<LocalDate, Int>
)

object EstadisticasReglas {

    const val SEMANAS_ACTIVIDAD = 12
    const val DIAS_ACTIVIDAD = SEMANAS_ACTIVIDAD * 7

    fun calcular(
        xpTotal: Int,
        conteos: Map<String, Int>,
        xpPorDia: Map<LocalDate, Int>,
        asistencias: List<Boolean>,
        sesiones: List<Pair<LocalDate, Int>>,
        mejorRacha: Int,
        hoy: LocalDate
    ): EstadisticasProgreso {
        val terminadas = conteos[TipoRecompensa.TAREA_COMPLETADA.name] ?: 0
        val aTiempo = conteos[TipoRecompensa.TAREA_A_TIEMPO.name] ?: 0
        val lunes = RachaReglas.lunes(hoy)

        fun xpEntre(desde: LocalDate, hasta: LocalDate) =
            xpPorDia.filterKeys { !it.isBefore(desde) && !it.isAfter(hasta) }.values.sum()

        return EstadisticasProgreso(
            xpTotal = xpTotal,
            tareasTerminadas = terminadas,
            porcentajeATiempo = porcentaje(aTiempo, terminadas),
            porcentajeAsistencia = porcentaje(asistencias.count { it }, asistencias.size),
            minutosEnfoqueTotal = sesiones.sumOf { it.second },
            minutosEnfoqueSemana = sesiones.filter { !it.first.isBefore(lunes) }.sumOf { it.second },
            mejorRacha = mejorRacha,
            xpSemana = xpEntre(lunes, hoy),
            xpSemanaAnterior = xpEntre(lunes.minusDays(7), lunes.minusDays(1)),
            ultimos7Dias = (6 downTo 0).map { hoy.minusDays(it.toLong()) }.map { it to (xpPorDia[it] ?: 0) },
            actividad = xpPorDia
        )
    }

    private fun porcentaje(parte: Int, total: Int): Int? =
        if (total == 0) null else (parte * 100 / total).coerceAtMost(100)
}
