package com.softcg.myapplication.domain.compartir

import com.softcg.myapplication.domain.CrearAsignaturaUseCase
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.AsignaturaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.EventoCompartido
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.NotaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.TareaCompartida
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Cómo se le describe al estudiante lo que comparte o lo que recibe. */
object TextosCompartir {

    private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val PRIORIDADES = mapOf(1 to "Alta", 2 to "Media", 3 to "Baja")

    fun titulo(contenido: ContenidoCompartido): String = when (contenido) {
        is TareaCompartida -> contenido.titulo
        is EventoCompartido -> contenido.titulo
        is AsignaturaCompartida -> contenido.nombre
        is NotaCompartida -> contenido.tipoNota.ifBlank { "Nota" }
    }

    /** Filas rótulo → valor; se omiten las vacías. */
    fun detalles(contenido: ContenidoCompartido): List<Pair<String, String>> = when (contenido) {
        is TareaCompartida -> listOf(
            "Asignatura" to contenido.asignatura,
            "Fecha de entrega" to fecha(contenido.fecha),
            "Prioridad" to PRIORIDADES[contenido.prioridad].orEmpty(),
            "Descripción" to contenido.descripcion
        )
        is EventoCompartido -> listOf(
            "Fecha" to fecha(contenido.fecha),
            "Horario" to listOf(contenido.horaInicio, contenido.horaFin).filter { it.isNotBlank() }.joinToString(" - "),
            "Prioridad" to PRIORIDADES[contenido.prioridad].orEmpty(),
            "Descripción" to contenido.descripcion
        )
        is AsignaturaCompartida -> listOf(
            "Profesor" to contenido.tutor,
            "Días" to CrearAsignaturaUseCase.DIAS.filterIndexed { i, _ -> contenido.dias.getOrElse(i) { false } }.joinToString(", "),
            "Hora de inicio" to contenido.hora,
            "Duración" to if (contenido.duracion > 0) "${contenido.duracion} h" else "",
            "Tipo de calificación" to contenido.tipoCalificacion,
            "Área" to contenido.area?.let(CatalogoModelo::etiqueta).orEmpty()
        )
        is NotaCompartida -> listOf(
            "Asignatura" to contenido.asignatura,
            "Calificación" to numero(contenido.valor),
            "Descripción" to contenido.descripcion
        )
    }.filter { it.second.isNotBlank() }

    /** El aviso tras confirmar en la ventana de lo que llegó por QR. */
    fun resultado(resultado: ResultadoImportacion): String = when (resultado) {
        is ResultadoImportacion.Importado -> {
            val tipo = resultado.tipo
            val agregado = "${tipo.nombre.replaceFirstChar { it.uppercase() }} agregad${if (tipo.articulo == "el") "o" else "a"}"
            resultado.asignaturaCreada?.let { "$agregado · se creó la asignatura «$it»" } ?: agregado
        }
        ResultadoImportacion.FaltaAsignatura -> "Elige a qué asignatura agregarla"
        ResultadoImportacion.AsignaturaRepetida -> "Ya tienes una asignatura con ese nombre"
        ResultadoImportacion.SinSesion -> "Inicia sesión para agregar lo que te compartieron"
        ResultadoImportacion.Error -> "No se pudo agregar. Inténtalo de nuevo"
    }

    /** "Compartir tarea", "Agregar evento"... */
    fun accion(verbo: String, tipo: TipoCompartido) = "$verbo ${tipo.nombre}"

    private fun fecha(texto: String) = runCatching { LocalDate.parse(texto).format(FECHA) }.getOrDefault(texto)

    private fun numero(valor: Float) = if (valor % 1f == 0f) valor.toInt().toString() else valor.toString()
}
