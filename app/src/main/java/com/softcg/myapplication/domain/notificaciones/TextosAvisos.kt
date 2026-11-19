package com.softcg.myapplication.domain.notificaciones

import com.softcg.myapplication.domain.gamificacion.ClaseProgramada
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Canal del sistema en el que se publica: el estudiante puede silenciar cada uno por separado en Ajustes. */
enum class CanalAviso { CLASES, TAREAS, RESUMEN, ASISTENCIA }

/** Color con el que se resalta (icono, acento y botones), de menos a más urgente. */
enum class TonoAviso { MARCA, EXITO, ATENCION, URGENTE }

/** Botones de la notificación; se resuelven sin abrir la app. */
enum class TipoAccionAviso { ASISTI, NO_ASISTI, ENTREGAR }

data class AccionAviso(val tipo: TipoAccionAviso, val etiqueta: String)

/** A qué pantalla lleva tocar la notificación. */
sealed class DestinoAviso {
    data class AbrirTarea(val tareaId: Int, val titulo: String, val asignatura: String) : DestinoAviso()
    object Horario : DestinoAviso()
    object ConfirmarAsistencia : DestinoAviso()
    object Inicio : DestinoAviso()
}

/** Lo que se muestra; la capa Android lo convierte en una notificación. */
data class ContenidoAviso(
    val claveNotificacion: String,
    val referencia: String,
    val canal: CanalAviso,
    val tono: TonoAviso,
    val titulo: String,
    val texto: String,
    /** Texto completo al desplegar; si es null se repite [texto]. */
    val detalle: String? = null,
    /** Una línea por elemento (resumen del día). */
    val lineas: List<String> = emptyList(),
    val acciones: List<AccionAviso> = emptyList(),
    val destino: DestinoAviso = DestinoAviso.Inicio,
    /** Momento al que se refiere (inicio de la clase, límite de la tarea…): se muestra en la notificación. */
    val momento: LocalDateTime? = null
)

/** Textos de cada aviso. Cortos y con lo esencial al principio: en la pantalla bloqueada se ve poco. */
object TextosAvisos {

    private val HORA = DateTimeFormatter.ofPattern("HH:mm")
    const val MAX_LINEAS_RESUMEN = 6

    fun clasePronto(clase: ClaseProgramada, tutor: String): ContenidoAviso {
        val referencia = ReglasAvisos.referenciaClase(clase.asignaturaId, clase.fecha)
        val horario = "${clase.inicio.format(HORA)} – ${clase.fin.format(HORA)}"
        val profesor = tutor.trim().takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()
        return ContenidoAviso(
            claveNotificacion = ReglasAvisos.claveNotificacion(TipoAviso.CLASE_PRONTO, referencia),
            referencia = referencia,
            canal = CanalAviso.CLASES,
            tono = TonoAviso.MARCA,
            titulo = "${clase.nombre} en ${ReglasAvisos.MINUTOS_ANTES_DE_CLASE} minutos",
            texto = "$horario$profesor",
            detalle = "Tu clase de ${clase.nombre} es de $horario$profesor. ¡Que te vaya muy bien!",
            destino = DestinoAviso.Horario,
            momento = clase.inicio
        )
    }

    fun asistencia(clase: ClaseProgramada): ContenidoAviso {
        val referencia = ReglasAvisos.referenciaClase(clase.asignaturaId, clase.fecha)
        val monedas = TipoRecompensa.ASISTENCIA.cantidad
        return ContenidoAviso(
            claveNotificacion = ReglasAvisos.claveNotificacion(TipoAviso.ASISTENCIA, referencia),
            referencia = referencia,
            canal = CanalAviso.ASISTENCIA,
            tono = TonoAviso.EXITO,
            titulo = "¿Fuiste a ${clase.nombre}?",
            texto = "Responde aquí y gana +$monedas EduCoins",
            detalle = "La clase terminó a las ${clase.fin.format(HORA)}. Responde aquí mismo: si fuiste, ganas +$monedas EduCoins. " +
                "Tienes hasta la medianoche.",
            acciones = listOf(
                AccionAviso(TipoAccionAviso.ASISTI, "Sí, asistí"),
                AccionAviso(TipoAccionAviso.NO_ASISTI, "No asistí")
            ),
            destino = DestinoAviso.ConfirmarAsistencia,
            momento = clase.fin
        )
    }

    fun tarea(tarea: Tarea, tipo: TipoAviso, limite: LocalDateTime): ContenidoAviso {
        val referencia = tarea.id.toString()
        val hora = limite.format(HORA)
        val asignatura = tarea.asignatura.ifBlank { "Tarea" }
        val (titulo, texto, detalle, tono) = when (tipo) {
            TipoAviso.TAREA_UNA_HORA -> Textos(
                "${tarea.titulo}",
                "Vence en 1 hora · $asignatura · $hora",
                "$asignatura · vence hoy a las $hora. Si ya la terminaste, márcala como entregada.",
                TonoAviso.ATENCION
            )
            TipoAviso.TAREA_CINCO_MINUTOS -> Textos(
                "${tarea.titulo}",
                "¡Quedan 5 minutos! · $asignatura · $hora",
                "$asignatura · vence a las $hora. ¡Último empujón! Si ya la entregaste, márcala aquí.",
                TonoAviso.URGENTE
            )
            else -> Textos(
                "${tarea.titulo}",
                "Se venció a las $hora · $asignatura",
                "$asignatura · venció a las $hora. Si ya la terminaste, márcala como entregada para llevar la cuenta.",
                TonoAviso.URGENTE
            )
        }
        return ContenidoAviso(
            claveNotificacion = ReglasAvisos.claveNotificacion(tipo, referencia),
            referencia = referencia,
            canal = CanalAviso.TAREAS,
            tono = tono,
            titulo = titulo,
            texto = texto,
            detalle = detalle,
            acciones = listOf(AccionAviso(TipoAccionAviso.ENTREGAR, "Marcar como entregada")),
            destino = DestinoAviso.AbrirTarea(tarea.id ?: -1, tarea.titulo, tarea.asignatura),
            momento = limite
        )
    }

    /** null si no hay nada ese día: un resumen vacío no aporta y acostumbra a ignorar las notificaciones. */
    fun resumenDelDia(fecha: String, tareas: List<Tarea>, eventos: List<Evento>): ContenidoAviso? {
        if (tareas.isEmpty() && eventos.isEmpty()) return null
        val partes = listOfNotNull(
            tareas.size.takeIf { it > 0 }?.let { if (it == 1) "1 tarea" else "$it tareas" },
            eventos.size.takeIf { it > 0 }?.let { if (it == 1) "1 evento" else "$it eventos" }
        )
        val lineas = tareas.map { tarea ->
            val hasta = tarea.horaEntrega?.takeIf { it.isNotBlank() }?.let { " · hasta las $it" }.orEmpty()
            "Tarea: ${tarea.titulo} · ${tarea.asignatura}$hasta"
        } + eventos.map { evento ->
            val horario = listOf(evento.horaInicio, evento.horaFin).filter { it.isNotBlank() }.joinToString(" – ")
            "Evento: ${evento.titulo}" + horario.takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()
        }
        val visibles = lineas.take(MAX_LINEAS_RESUMEN) +
            listOfNotNull((lineas.size - MAX_LINEAS_RESUMEN).takeIf { it > 0 }?.let { "y $it más…" })
        return ContenidoAviso(
            claveNotificacion = ReglasAvisos.claveNotificacion(TipoAviso.RESUMEN_DIA, fecha),
            referencia = fecha,
            canal = CanalAviso.RESUMEN,
            tono = TonoAviso.MARCA,
            titulo = "Buenos días: hoy tienes ${partes.joinToString(" y ")}",
            texto = (tareas.map { it.titulo } + eventos.map { it.titulo }).let { nombres ->
                nombres.first() + if (nombres.size > 1) " y ${nombres.size - 1} más" else ""
            },
            lineas = visibles,
            destino = DestinoAviso.Inicio
        )
    }

    /** Lo que muestra la notificación después de pulsar un botón. */
    fun confirmacionAsistencia(asistio: Boolean, monedas: Int): Pair<String, String> =
        if (asistio) "¡Asistencia registrada!" to (if (monedas > 0) "+$monedas EduCoins por ir a clase" else "Se guardó tu respuesta")
        else "Respuesta guardada" to "Registramos que no asististe. ¡La próxima vez será!"

    fun confirmacionEntrega(monedas: Int, aTiempo: Boolean): Pair<String, String> =
        "¡Tarea entregada!" to when {
            monedas > 0 && aTiempo -> "+$monedas EduCoins, ¡a tiempo!"
            monedas > 0 -> "+$monedas EduCoins"
            else -> "Quedó marcada como entregada"
        }

    private data class Textos(val titulo: String, val texto: String, val detalle: String, val tono: TonoAviso)
}
