package com.softcg.myapplication.domain.compartir

import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea

/**
 * Lo que viaja en un QR de EduPlanner. Solo la información que tiene sentido en otro dispositivo:
 * ni ids, ni estado (entregada, calificada), ni datos personales de predicción (interés, sesiones),
 * ni la imagen de un evento (es un archivo local del teléfono que la comparte).
 */
sealed class ContenidoCompartido {
    abstract val tipo: TipoCompartido

    /** Nombre de la asignatura a la que pertenece en el dispositivo que lo comparte; null si no tiene. */
    open val asignatura: String? get() = null

    data class TareaCompartida(
        val titulo: String,
        val descripcion: String,
        override val asignatura: String,
        val fecha: String,
        val prioridad: Int,
        /** "HH:mm" o vacía (vence a las 23:59). */
        val horaEntrega: String = ""
    ) : ContenidoCompartido() {
        override val tipo get() = TipoCompartido.TAREA
    }

    data class EventoCompartido(
        val titulo: String,
        val descripcion: String,
        val fecha: String,
        val horaInicio: String,
        val horaFin: String,
        val prioridad: Int
    ) : ContenidoCompartido() {
        override val tipo get() = TipoCompartido.EVENTO
    }

    data class AsignaturaCompartida(
        val nombre: String,
        val tutor: String,
        val duracion: Int,
        val hora: String,
        /** Lunes a domingo. */
        val dias: List<Boolean>,
        val tipoCalificacion: String,
        val area: String?,
        val escalaMin: Float?,
        val escalaMax: Float?
    ) : ContenidoCompartido() {
        override val tipo get() = TipoCompartido.ASIGNATURA
    }

    data class NotaCompartida(
        val tipoNota: String,
        val valor: Float,
        override val asignatura: String,
        val descripcion: String
    ) : ContenidoCompartido() {
        override val tipo get() = TipoCompartido.NOTA
    }

    companion object {
        fun de(tarea: Tarea) = TareaCompartida(tarea.titulo, tarea.descrip, tarea.asignatura, tarea.fecha, tarea.prioridad, tarea.horaEntrega.orEmpty())

        fun de(evento: Evento) =
            EventoCompartido(evento.titulo, evento.descrip, evento.fecha, evento.horaInicio, evento.horaFin, evento.prioridad)

        fun de(asignatura: Asignatura) = AsignaturaCompartida(
            asignatura.nombre, asignatura.tutor, asignatura.duracion, asignatura.hora, asignatura.horario,
            asignatura.tipoCalificacion, asignatura.areaModelo, asignatura.escalaMin, asignatura.escalaMax
        )

        fun de(nota: Calificacion) = NotaCompartida(nota.tipo, nota.valor, nota.asignatura, nota.descripcion)
    }
}

/** [clave] es lo que se escribe en el QR: no cambiarla, los códigos ya impresos dejarían de leerse. */
enum class TipoCompartido(val clave: String, val nombre: String, val articulo: String) {
    TAREA("t", "tarea", "la"),
    EVENTO("e", "evento", "el"),
    ASIGNATURA("a", "asignatura", "la"),
    NOTA("n", "nota", "la");

    companion object {
        fun deClave(clave: String): TipoCompartido? = entries.firstOrNull { it.clave == clave }
    }
}
