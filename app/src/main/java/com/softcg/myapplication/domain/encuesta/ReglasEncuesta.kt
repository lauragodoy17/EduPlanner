package com.softcg.myapplication.domain.encuesta

import java.math.BigDecimal
import java.math.RoundingMode

/** Respuestas por id de pregunta ("S2_3" → "4"); el texto de "Otra app" va en "S1_6_detalle". */
typealias RespuestasEncuesta = Map<String, String>

/** Una celda de la fila que se escribe en la hoja: el encabezado de su columna y su valor (número o texto). */
data class ColumnaEncuesta(val encabezado: String, val valor: Any?)

/** Datos de la respuesta que no son preguntas. Anónimos: no se envía el correo ni el uid. */
data class MetaEncuesta(val fecha: String, val idRespuesta: String, val participante: String, val version: String)

/** Validación, puntajes y fila de la hoja de la encuesta. Puro: se prueba sin Android. */
object ReglasEncuesta {

    const val COL_FECHA = "Fecha de envío"
    const val COL_ID = "ID de respuesta"
    const val COL_PARTICIPANTE = "Participante (anónimo)"
    const val COL_VERSION = "Versión de la app"
    const val COL_SUS = "SUS (0-100)"
    const val COL_NPS_CATEGORIA = "NPS categoría"
    const val PREFIJO_PROMEDIO = "Promedio "

    /** La recomendación de 0 a 10, de la que sale la categoría NPS. */
    const val ID_NPS = "S9_3"

    private const val SUFIJO_DETALLE = "_detalle"

    fun claveDetalle(pregunta: Pregunta.Opcion) = pregunta.id + SUFIJO_DETALLE

    fun visible(pregunta: Pregunta, respuestas: RespuestasEncuesta): Boolean =
        pregunta.soloSi?.let { respuestas[it.preguntaId] == it.valor } ?: true

    /** La respuesta guardada solo cuenta si es válida para su pregunta (un borrador viejo o alterado no pasa). */
    fun valida(pregunta: Pregunta, valor: String?): Boolean {
        if (valor.isNullOrBlank()) return false
        return when (pregunta) {
            is Pregunta.Likert -> valor.toIntOrNull() in 1..5
            is Pregunta.Puntaje -> valor.toIntOrNull()?.let { it in pregunta.rango } == true
            is Pregunta.Opcion -> valor in pregunta.opciones
            is Pregunta.Texto -> valor.trim().length <= pregunta.maximo
        }
    }

    /** Preguntas visibles de la sección que faltan por responder bien (incluida "¿Cuál app?" si eligió "Otra app"). */
    fun faltantes(seccion: SeccionEncuesta, respuestas: RespuestasEncuesta): List<String> =
        seccion.preguntas.filter { visible(it, respuestas) }.filter { pregunta ->
            val valor = respuestas[pregunta.id]
            val faltaRespuesta = pregunta.obligatoria && !valida(pregunta, valor)
            val textoLargo = pregunta is Pregunta.Texto && !valor.isNullOrBlank() && !valida(pregunta, valor)
            val faltaDetalle = pregunta is Pregunta.Opcion && pregunta.conDetalle != null && valor == pregunta.conDetalle &&
                respuestas[claveDetalle(pregunta)].isNullOrBlank()
            faltaRespuesta || textoLargo || faltaDetalle
        }.map { it.id }

    /** Primera sección con algo pendiente, o null si la encuesta está completa. */
    fun primeraIncompleta(respuestas: RespuestasEncuesta): Int? =
        EncuestaCatalogo.secciones.indexOfFirst { faltantes(it, respuestas).isNotEmpty() }.takeIf { it >= 0 }

    /**
     * System Usability Scale: ítems impares (respuesta − 1), pares (5 − respuesta), suma × 2,5 → 0 a 100.
     * Null si falta algún ítem.
     */
    fun sus(respuestas: RespuestasEncuesta): Double? {
        val puntos = (1..10).map { item ->
            val valor = respuestas["S2_$item"]?.toIntOrNull()?.takeIf { it in 1..5 } ?: return null
            if (item % 2 == 1) valor - 1 else 5 - valor
        }
        return puntos.sum() * 2.5
    }

    /** 9–10 promotor, 7–8 pasivo, 0–6 detractor. */
    fun categoriaNps(valor: Int?): String? = when (valor) {
        null -> null
        in 9..10 -> "Promotor"
        in 7..8 -> "Pasivo"
        in 0..6 -> "Detractor"
        else -> null
    }

    /**
     * Media de las preguntas de 1 a 5 respondidas de la sección (dos decimales); en una sección sin ellas (Impacto),
     * de sus puntajes. Null si no hay ninguna (p. ej. QR sin usar).
     */
    fun promedio(seccion: SeccionEncuesta, respuestas: RespuestasEncuesta): Double? {
        val visibles = seccion.preguntas.filter { visible(it, respuestas) }
        val cuentan: List<Pregunta> = visibles.filterIsInstance<Pregunta.Likert>()
            .ifEmpty { visibles.filterIsInstance<Pregunta.Puntaje>() }
        val valores = cuentan.mapNotNull { p -> respuestas[p.id]?.takeIf { valida(p, it) }?.toInt() }
        if (valores.isEmpty()) return null
        return BigDecimal(valores.sum()).divide(BigDecimal(valores.size), 2, RoundingMode.HALF_UP).toDouble()
    }

    fun encabezado(pregunta: Pregunta): String = pregunta.id.replace('_', '.') + " " + pregunta.texto

    /**
     * La fila completa, en el orden de la hoja: datos de la respuesta, cada pregunta (las ocultas, vacías), el
     * puntaje SUS, la categoría NPS y el promedio de cada módulo. Los números van como números para que la hoja
     * pueda promediarlos.
     */
    fun columnas(respuestas: RespuestasEncuesta, meta: MetaEncuesta): List<ColumnaEncuesta> {
        val filas = mutableListOf(
            ColumnaEncuesta(COL_FECHA, meta.fecha),
            ColumnaEncuesta(COL_ID, meta.idRespuesta),
            ColumnaEncuesta(COL_PARTICIPANTE, meta.participante),
            ColumnaEncuesta(COL_VERSION, meta.version)
        )
        EncuestaCatalogo.preguntas.forEach { pregunta ->
            val valor = respuestas[pregunta.id]?.trim()?.takeIf { visible(pregunta, respuestas) && valida(pregunta, it) }
            filas += ColumnaEncuesta(
                encabezado(pregunta),
                when (pregunta) {
                    is Pregunta.Likert, is Pregunta.Puntaje -> valor?.toInt()
                    else -> valor.orEmpty()
                }
            )
            if (pregunta is Pregunta.Opcion && pregunta.conDetalle != null) {
                val detalle = respuestas[claveDetalle(pregunta)]?.trim()?.take(MAX_DETALLE)
                    ?.takeIf { valor == pregunta.conDetalle }
                filas += ColumnaEncuesta("${encabezado(pregunta)} (${pregunta.detalleHint})", detalle.orEmpty())
            }
        }
        filas += ColumnaEncuesta(COL_SUS, sus(respuestas))
        filas += ColumnaEncuesta(COL_NPS_CATEGORIA, categoriaNps(respuestas[ID_NPS]?.toIntOrNull()).orEmpty())
        EncuestaCatalogo.secciones.filter { it.promedio != null }.forEach { seccion ->
            filas += ColumnaEncuesta(PREFIJO_PROMEDIO + seccion.promedio, promedio(seccion, respuestas))
        }
        return filas
    }

    const val MAX_DETALLE = 60
}
