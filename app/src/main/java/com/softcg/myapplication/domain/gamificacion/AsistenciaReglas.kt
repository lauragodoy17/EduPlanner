package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Una clase concreta: una asignatura en una fecha en la que tiene clase. */
data class ClaseProgramada(
    val asignaturaId: Int,
    val nombre: String,
    val fecha: LocalDate,
    val inicio: LocalDateTime,
    val fin: LocalDateTime
) {
    /** 00:00 del día siguiente al de finalización (si la clase cruza la medianoche, cuenta el día en que termina). */
    val plazo: LocalDateTime get() = fin.toLocalDate().plusDays(1).atStartOfDay()
}

enum class EstadoAsistencia { PENDIENTE, DISPONIBLE, ASISTI, NO_ASISTI, VENCIDA }

object AsistenciaReglas {

    /** Acepta "14:00", "8:5" o "8: 5" (formatos que guarda la app). */
    fun parsearHora(hora: String): LocalTime? {
        val partes = hora.replace(" ", "").split(":")
        if (partes.size != 2) return null
        val h = partes[0].toIntOrNull() ?: return null
        val m = partes[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return LocalTime.of(h, m)
    }

    /**
     * La clase de [asignatura] en [fecha], o null si ese día no tiene clase, sus datos están incompletos
     * o empezó antes de registrar la asignatura (no se puede registrar una clase y confirmarla a posteriori).
     */
    fun claseEnFecha(asignatura: Asignatura, fecha: LocalDate, zona: ZoneId): ClaseProgramada? {
        val id = asignatura.id ?: return null
        if (asignatura.horario.size != 7 || !asignatura.horario[fecha.dayOfWeek.value - 1]) return null
        if (asignatura.duracion <= 0) return null
        val hora = parsearHora(asignatura.hora) ?: return null

        val inicio = fecha.atTime(hora)
        val registro = Instant.ofEpochMilli(asignatura.fechaRegistro).atZone(zona).toLocalDateTime()
        if (inicio.isBefore(registro)) return null

        return ClaseProgramada(id, asignatura.nombre, fecha, inicio, inicio.plusHours(asignatura.duracion.toLong()))
    }

    /** [respuesta]: true = asistió, false = no asistió, null = sin responder. */
    fun estado(clase: ClaseProgramada, respuesta: Boolean?, ahora: LocalDateTime): EstadoAsistencia = when {
        respuesta == true -> EstadoAsistencia.ASISTI
        respuesta == false -> EstadoAsistencia.NO_ASISTI
        ahora.isBefore(clase.fin) -> EstadoAsistencia.PENDIENTE
        ahora.isBefore(clase.plazo) -> EstadoAsistencia.DISPONIBLE
        else -> EstadoAsistencia.VENCIDA
    }

    /** Clases que se pueden confirmar ahora y aún no tienen respuesta, de la que terminó antes a la última. */
    fun clasesPorConfirmar(
        asignaturas: List<Asignatura>,
        respondidas: Set<Pair<Int, LocalDate>>,
        ahora: LocalDateTime,
        zona: ZoneId
    ): List<ClaseProgramada> {
        val hoy = ahora.toLocalDate()
        return asignaturas.flatMap { asignatura ->
            // Una clase disponible termina hoy; si dura más de un día pudo empezar varios días antes
            val diasAtras = asignatura.duracion.coerceAtLeast(0) / 24 + 1
            (0..diasAtras).mapNotNull { claseEnFecha(asignatura, hoy.minusDays(it.toLong()), zona) }
        }
            .filter { (it.asignaturaId to it.fecha) !in respondidas }
            .filter { estado(it, null, ahora) == EstadoAsistencia.DISPONIBLE }
            .sortedWith(compareBy({ it.fin }, { it.nombre }))
    }
}
