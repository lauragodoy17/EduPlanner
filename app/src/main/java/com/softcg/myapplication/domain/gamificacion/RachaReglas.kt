package com.softcg.myapplication.domain.gamificacion

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * [actual] y [mejor] cuentan días con actividad. [enRiesgo]: hoy aún no hay actividad y, si termina
 * el día sin ella, la racha se perdería porque el descanso de esta semana ya se usó.
 */
data class Racha(
    val actual: Int,
    val mejor: Int,
    val activaHoy: Boolean,
    val descansoDisponible: Boolean,
    val enRiesgo: Boolean
)

/**
 * Se perdona un día sin actividad por semana (lunes a domingo): un estudiante también descansa,
 * y perder una racha larga por un domingo desmotiva más de lo que premia.
 */
object RachaReglas {

    fun calcular(diasActivos: Set<LocalDate>, hoy: LocalDate): Racha {
        val dias = diasActivos.filter { !it.isAfter(hoy) }.sorted()
        if (dias.isEmpty()) return Racha(0, 0, activaHoy = false, descansoDisponible = true, enRiesgo = false)

        var racha = 0
        var mejor = 0
        val semanasConDescanso = mutableSetOf<LocalDate>()
        var anterior: LocalDate? = null

        for (dia in dias) {
            val previo = anterior
            if (previo != null && !perdonarHueco(previo.plusDays(1), dia, semanasConDescanso)) {
                racha = 0
                semanasConDescanso.clear()
            }
            racha++
            mejor = maxOf(mejor, racha)
            anterior = dia
        }

        val ultimo = dias.last()
        val activaHoy = ultimo == hoy
        // Hoy no cuenta como hueco: el día aún no termina
        val viva = activaHoy || perdonarHueco(ultimo.plusDays(1), hoy, semanasConDescanso)
        val actual = if (viva) racha else 0
        val descansoDisponible = lunes(hoy) !in semanasConDescanso

        return Racha(
            actual = actual,
            mejor = mejor,
            activaHoy = activaHoy,
            descansoDisponible = descansoDisponible,
            enRiesgo = actual > 0 && !activaHoy && !descansoDisponible
        )
    }

    /** Días sin actividad en [desde, hasta); gasta el descanso de cada semana y falla si no alcanza. */
    private fun perdonarHueco(desde: LocalDate, hasta: LocalDate, semanasConDescanso: MutableSet<LocalDate>): Boolean {
        var dia = desde
        while (dia.isBefore(hasta)) {
            if (!semanasConDescanso.add(lunes(dia))) return false
            dia = dia.plusDays(1)
        }
        return true
    }

    fun lunes(dia: LocalDate): LocalDate = dia.minusDays((dia.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
}
