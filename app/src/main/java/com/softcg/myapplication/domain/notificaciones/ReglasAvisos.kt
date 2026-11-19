package com.softcg.myapplication.domain.notificaciones

import com.softcg.myapplication.domain.gamificacion.AsistenciaReglas
import com.softcg.myapplication.domain.gamificacion.ClaseProgramada
import com.softcg.myapplication.domain.gamificacion.ReglasEntrega
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Qué se avisa. [clave] viaja en las alarmas ya programadas en el teléfono: no cambiarla.
 * Las tres de una tarea comparten notificación (cada una reemplaza a la anterior), igual que las dos de una clase.
 */
enum class TipoAviso(val clave: String) {
    CLASE_PRONTO("clase"),
    ASISTENCIA("asistencia"),
    RESUMEN_DIA("resumen"),
    TAREA_UNA_HORA("tarea1h"),
    TAREA_CINCO_MINUTOS("tarea5m"),
    TAREA_VENCIDA("vencida");

    companion object {
        fun deClave(clave: String?): TipoAviso? = entries.firstOrNull { it.clave == clave }
    }
}

/**
 * Un aviso concreto que hay que mostrar en [momento]. [referencia] identifica lo avisado:
 * "asignaturaId:fecha" para clases, el id para tareas y la fecha para el resumen del día.
 */
data class Aviso(val tipo: TipoAviso, val referencia: String, val momento: LocalDateTime) {
    /** Identifica la alarma: una por aviso. */
    val claveAlarma: String get() = "${tipo.clave}:$referencia"

    /** Identifica la notificación en pantalla: la comparten los avisos de una misma tarea o clase. */
    val claveNotificacion: String get() = ReglasAvisos.claveNotificacion(tipo, referencia)
}

/**
 * [programar]: las alarmas que deben quedar puestas. [vigentes]: las notificaciones que, si ya se ven,
 * siguen teniendo sentido; las demás se retiran (tarea entregada o borrada, asistencia ya respondida…).
 */
data class PlanAvisos(val programar: List<Aviso>, val vigentes: Set<String>)

/** Cuándo se avisa. Reglas puras:  lo que dependa de la hora llega como parámetro. */
object ReglasAvisos {

    const val MINUTOS_ANTES_DE_CLASE = 10L
    val HORA_RESUMEN: LocalTime = LocalTime.of(9, 0)
    const val MINUTOS_ANTES_UNA_HORA = 60L
    const val MINUTOS_ANTES_CINCO = 5L

    /** El aviso de asistencia sale justo después de terminar la clase. */
    const val MINUTOS_TRAS_CLASE = 1L

    /** El de "venció" sale un minuto después del límite, cuando ya no está a tiempo. */
    const val MINUTOS_TRAS_VENCER = 1L

    /** Clases de hoy y de los próximos días: el resumen de cada mañana vuelve a planificar. */
    const val DIAS_DE_CLASES = 2L

    fun referenciaClase(asignaturaId: Int, fecha: LocalDate) = "$asignaturaId:$fecha"

    fun leerReferenciaClase(referencia: String): Pair<Int, LocalDate>? {
        val partes = referencia.split(":")
        if (partes.size != 2) return null
        val id = partes[0].toIntOrNull() ?: return null
        val fecha = runCatching { LocalDate.parse(partes[1]) }.getOrNull() ?: return null
        return id to fecha
    }

    fun claveNotificacion(tipo: TipoAviso, referencia: String): String = when (tipo) {
        TipoAviso.CLASE_PRONTO, TipoAviso.ASISTENCIA -> "clase:$referencia"
        TipoAviso.TAREA_UNA_HORA, TipoAviso.TAREA_CINCO_MINUTOS, TipoAviso.TAREA_VENCIDA -> "tarea:$referencia"
        TipoAviso.RESUMEN_DIA -> "resumen:$referencia"
    }

    fun pendiente(tarea: Tarea) = tarea.id != null && tarea.calificacion == null && tarea.entregadaMillis == null

    /** Las clases de [asignaturas] entre ayer (para la asistencia de una clase nocturna) y [DIAS_DE_CLASES] días adelante. */
    fun clasesCercanas(asignaturas: List<Asignatura>, hoy: LocalDate, zona: ZoneId): List<ClaseProgramada> =
        (-1L..DIAS_DE_CLASES).flatMap { dias ->
            asignaturas.mapNotNull { AsistenciaReglas.claseEnFecha(it, hoy.plusDays(dias), zona) }
        }

    fun planificar(
        tareas: List<Tarea>,
        asignaturas: List<Asignatura>,
        respondidas: Set<Pair<Int, LocalDate>>,
        ahora: LocalDateTime,
        zona: ZoneId
    ): PlanAvisos {
        val avisos = mutableListOf<Aviso>()
        val vigentes = mutableSetOf<String>()
        val hoy = ahora.toLocalDate()

        // Resumen de las 9:00: el de hoy si aún no ha pasado, si no el de mañana. El de hoy sigue vigente  el día
        val resumenHoy = hoy.atTime(HORA_RESUMEN)
        val proximoResumen = if (ahora.isBefore(resumenHoy)) resumenHoy else resumenHoy.plusDays(1)
        avisos += Aviso(TipoAviso.RESUMEN_DIA, proximoResumen.toLocalDate().toString(), proximoResumen)
        vigentes += claveNotificacion(TipoAviso.RESUMEN_DIA, hoy.toString())

        for (clase in clasesCercanas(asignaturas, hoy, zona)) {
            val referencia = referenciaClase(clase.asignaturaId, clase.fecha)
            val pronto = Aviso(TipoAviso.CLASE_PRONTO, referencia, clase.inicio.minusMinutes(MINUTOS_ANTES_DE_CLASE))
            if (pronto.momento.isAfter(ahora)) avisos += pronto

            val respondida = (clase.asignaturaId to clase.fecha) in respondidas
            if (!respondida) {
                val asistencia = Aviso(TipoAviso.ASISTENCIA, referencia, clase.fin.plusMinutes(MINUTOS_TRAS_CLASE))
                if (asistencia.momento.isAfter(ahora)) avisos += asistencia
            }
            // "Empieza en 10 minutos" vale hasta que termina la clase; la pregunta, hasta que se responde o vence
            val vigente = if (respondida) ahora.isBefore(clase.inicio) else ahora.isBefore(clase.plazo)
            if (vigente) vigentes += claveNotificacion(TipoAviso.CLASE_PRONTO, referencia)
        }

        for (tarea in tareas.filter(::pendiente)) {
            val limite = ReglasEntrega.limite(tarea.fecha, tarea.horaEntrega) ?: continue
            val referencia = tarea.id.toString()
            listOf(
                Aviso(TipoAviso.TAREA_UNA_HORA, referencia, limite.minusMinutes(MINUTOS_ANTES_UNA_HORA)),
                Aviso(TipoAviso.TAREA_CINCO_MINUTOS, referencia, limite.minusMinutes(MINUTOS_ANTES_CINCO)),
                Aviso(TipoAviso.TAREA_VENCIDA, referencia, limite.plusMinutes(MINUTOS_TRAS_VENCER))
            ).filterTo(avisos) { it.momento.isAfter(ahora) }
            vigentes += claveNotificacion(TipoAviso.TAREA_UNA_HORA, referencia)
        }

        return PlanAvisos(avisos.sortedBy { it.momento }, vigentes)
    }
}
