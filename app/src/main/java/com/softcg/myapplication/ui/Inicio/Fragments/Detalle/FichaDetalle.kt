package com.softcg.myapplication.ui.Inicio.Fragments.Detalle

import com.softcg.myapplication.domain.CrearAsignaturaUseCase
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.AsignaturaConHora
import com.softcg.myapplication.ui.Inicio.Models.Evento
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Lo que muestra la pantalla de detalle. [clave] viaja en los argumentos de navegación: no cambiarla. */
enum class TipoDetalle(val clave: String, val etiqueta: String, val pantalla: String, val articulo: String) {
    EVENTO("evento", "EVENTO", "Evento", "el evento"),
    ASIGNATURA("asignatura", "ASIGNATURA", "Asignatura", "la asignatura"),
    NOTA("nota", "CALIFICACIÓN", "Calificación", "la calificación");

    companion object {
        fun deClave(clave: String?): TipoDetalle? = entries.firstOrNull { it.clave == clave }
    }
}

/** La tarjeta de un evento, asignatura o nota: título, descripción y filas rótulo → valor (sin las vacías). */
data class Ficha(
    val tipo: TipoDetalle,
    val titulo: String,
    val descripcion: String,
    val filas: List<Pair<String, String>>,
    val imagenUri: String? = null
)

/** Arma la ficha de cada tipo con el mismo formato que la información de las tareas. */
object FichaDetalle {

    private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val PRIORIDADES = mapOf(1 to "Alta", 2 to "Media", 3 to "Baja")

    fun de(evento: Evento, hoy: LocalDate): Ficha = Ficha(
        tipo = TipoDetalle.EVENTO,
        titulo = evento.titulo.trim(),
        descripcion = evento.descrip.trim(),
        filas = listOf(
            "Fecha" to fechaEvento(evento.fecha, hoy),
            "Horario" to listOf(evento.horaInicio, evento.horaFin).filter { it.isNotBlank() }.joinToString(" - "),
            "Prioridad" to PRIORIDADES[evento.prioridad].orEmpty()
        ).sinVacias(),
        imagenUri = evento.imagenUri?.takeIf { it.isNotBlank() }
    )

    fun de(asignatura: Asignatura): Ficha {
        val dias = CrearAsignaturaUseCase.DIAS.filterIndexed { i, _ -> asignatura.horario.getOrElse(i) { false } }
        return Ficha(
            tipo = TipoDetalle.ASIGNATURA,
            titulo = asignatura.nombre.trim(),
            descripcion = "",
            filas = listOf(
                "Profesor" to asignatura.tutor.trim(),
                "Días de clase" to dias.joinToString(", ").ifEmpty { "Sin días asignados" },
                "Hora de inicio" to AsignaturaConHora.horaLegible(asignatura.hora),
                "Duración" to if (asignatura.duracion > 0) "${asignatura.duracion} h" else "",
                "Tipo de calificación" to asignatura.tipoCalificacion,
                "Área" to asignatura.areaModelo?.let(CatalogoModelo::etiqueta).orEmpty()
            ).sinVacias()
        )
    }

    fun de(calificacion: Calificacion): Ficha = Ficha(
        tipo = TipoDetalle.NOTA,
        titulo = calificacion.asignatura.trim().ifEmpty { "Calificación" },
        descripcion = calificacion.descripcion.trim(),
        filas = listOf(
            "Calificación" to numero(calificacion.valor),
            "Tipo" to calificacion.tipo
        ).sinVacias()
    )

    /** "01/12/2026 · faltan 3 días"; para los que ya pasaron, "fue hace 2 días". */
    fun fechaEvento(texto: String, hoy: LocalDate): String {
        val fecha = runCatching { LocalDate.parse(texto) }.getOrNull() ?: return texto
        val plazo = when (val dias = ChronoUnit.DAYS.between(hoy, fecha)) {
            0L -> "hoy"
            1L -> "mañana"
            -1L -> "fue ayer"
            in 2L..Long.MAX_VALUE -> "faltan $dias días"
            else -> "fue hace ${-dias} días"
        }
        return "${fecha.format(FECHA)} · $plazo"
    }

    private fun numero(valor: Float) = if (valor % 1f == 0f) valor.toInt().toString() else valor.toString()

    private fun List<Pair<String, String>>.sinVacias() = filter { it.second.isNotBlank() }
}
