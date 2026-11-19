package com.softcg.myapplication.domain.compartir

import com.softcg.myapplication.domain.compartir.ContenidoCompartido.AsignaturaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.EventoCompartido
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.NotaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.TareaCompartida
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Base64

sealed class LecturaQr {
    data class Valida(val contenido: ContenidoCompartido) : LecturaQr()

    /** No empieza por el prefijo de EduPlanner: es un QR de otra cosa (una web, un pago...). */
    object NoEsEduPlanner : LecturaQr()

    /** Es de EduPlanner pero de una versión más nueva de la app. */
    object VersionNueva : LecturaQr()

    /** Tiene el prefijo pero el contenido está dañado o incompleto. */
    object Danado : LecturaQr()
}

/**
 * Formato de los QR de EduPlanner: `EDUPLANNER:<versión>:<JSON en Base64 URL-safe>`.
 * El prefijo permite descartar en el acto cualquier otro QR; la versión, que una app antigua no malinterprete
 * uno generado por una más nueva. Al leer se valida y se acota cada campo: el QR puede venir de cualquiera.
 */
object CodigoQr {
    const val PREFIJO = "EDUPLANNER:"
    const val VERSION = 1

    // Límites al leer: el QR no lo genera necesariamente esta app
    private const val MAX_TITULO = 120
    private const val MAX_TEXTO = 400

    fun esDeEduPlanner(texto: String): Boolean = texto.startsWith(PREFIJO)

    fun codificar(contenido: ContenidoCompartido): String {
        val json = JSONObject().put("k", contenido.tipo.clave)
        when (contenido) {
            is TareaCompartida -> json
                .put("ti", contenido.titulo).put("de", contenido.descripcion).put("as", contenido.asignatura)
                .put("fe", contenido.fecha).put("pr", contenido.prioridad)
                .apply { if (contenido.horaEntrega.isNotBlank()) put("hr", contenido.horaEntrega) }
            is EventoCompartido -> json
                .put("ti", contenido.titulo).put("de", contenido.descripcion).put("fe", contenido.fecha)
                .put("hi", contenido.horaInicio).put("hf", contenido.horaFin).put("pr", contenido.prioridad)
            is AsignaturaCompartida -> json
                .put("no", contenido.nombre).put("tu", contenido.tutor).put("du", contenido.duracion)
                .put("ho", contenido.hora).put("di", JSONArray(contenido.dias)).put("tc", contenido.tipoCalificacion)
                .apply {
                    contenido.area?.let { put("ar", it) }
                    contenido.escalaMin?.let { put("mi", it.toDouble()) }
                    contenido.escalaMax?.let { put("ma", it.toDouble()) }
                }
            is NotaCompartida -> json
                .put("tn", contenido.tipoNota).put("va", contenido.valor.toDouble()).put("as", contenido.asignatura)
                .put("de", contenido.descripcion)
        }
        val datos = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
        return "$PREFIJO$VERSION:$datos"
    }

    fun leer(texto: String): LecturaQr {
        if (!esDeEduPlanner(texto)) return LecturaQr.NoEsEduPlanner
        val partes = texto.removePrefix(PREFIJO).split(':', limit = 2)
        val version = partes.firstOrNull()?.toIntOrNull() ?: return LecturaQr.Danado
        if (version > VERSION) return LecturaQr.VersionNueva
        if (partes.size < 2) return LecturaQr.Danado
        return runCatching {
            val json = JSONObject(String(Base64.getUrlDecoder().decode(partes[1]), Charsets.UTF_8))
            contenidoDe(json)?.let { LecturaQr.Valida(it) } ?: LecturaQr.Danado
        }.getOrDefault(LecturaQr.Danado)
    }

    private fun contenidoDe(json: JSONObject): ContenidoCompartido? {
        return when (TipoCompartido.deClave(json.optString("k"))) {
            TipoCompartido.TAREA -> TareaCompartida(
                titulo = json.texto("ti", MAX_TITULO).ifBlank { return null },
                descripcion = json.texto("de", MAX_TEXTO),
                asignatura = json.texto("as", MAX_TITULO),
                fecha = json.fecha("fe") ?: return null,
                prioridad = json.prioridad(),
                horaEntrega = json.hora("hr")
            )
            TipoCompartido.EVENTO -> EventoCompartido(
                titulo = json.texto("ti", MAX_TITULO).ifBlank { return null },
                descripcion = json.texto("de", MAX_TEXTO),
                fecha = json.fecha("fe") ?: return null,
                horaInicio = json.hora("hi"),
                horaFin = json.hora("hf"),
                prioridad = json.prioridad()
            )
            TipoCompartido.ASIGNATURA -> {
                val dias = json.optJSONArray("di")
                AsignaturaCompartida(
                    nombre = json.texto("no", MAX_TITULO).ifBlank { return null },
                    tutor = json.texto("tu", MAX_TITULO),
                    duracion = json.optInt("du", 0).coerceIn(0, 12),
                    hora = json.hora("ho"),
                    dias = List(7) { dias?.optBoolean(it, false) ?: false },
                    tipoCalificacion = json.texto("tc", MAX_TITULO),
                    area = json.optString("ar").takeIf { CatalogoModelo.esArea(it) },
                    escalaMin = json.decimal("mi"),
                    escalaMax = json.decimal("ma")
                ).let { a -> if (a.escalaMin != null && a.escalaMax != null && a.escalaMax > a.escalaMin) a else a.copy(escalaMin = null, escalaMax = null) }
            }
            TipoCompartido.NOTA -> NotaCompartida(
                tipoNota = json.texto("tn", MAX_TITULO),
                valor = json.decimal("va")?.takeIf { it >= 0f } ?: return null,
                asignatura = json.texto("as", MAX_TITULO).ifBlank { return null },
                descripcion = json.texto("de", MAX_TEXTO)
            )
            null -> null
        }
    }

    private fun JSONObject.texto(clave: String, max: Int) = optString(clave).trim().take(max)

    private fun JSONObject.fecha(clave: String): String? =
        optString(clave).takeIf { runCatching { LocalDate.parse(it) }.isSuccess }

    private fun JSONObject.hora(clave: String): String =
        optString(clave).trim().takeIf { Regex("""\d{1,2}:\d{2}""").matches(it) }.orEmpty()

    private fun JSONObject.prioridad() = optInt("pr", 0).coerceIn(0, 3)

    private fun JSONObject.decimal(clave: String): Float? =
        if (has(clave)) optDouble(clave).takeIf { !it.isNaN() && !it.isInfinite() }?.toFloat() else null
}
