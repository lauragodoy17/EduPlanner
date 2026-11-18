package com.softcg.myapplication.data.network

import android.util.Log
import com.softcg.myapplication.domain.ia.Fuente
import com.softcg.myapplication.domain.ia.FuentePropuesta
import com.softcg.myapplication.domain.ia.TipoFuente
import com.softcg.myapplication.domain.ia.VerificadorFuentes
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** Respuesta mínima de una petición GET: lo que hace falta para comprobar una fuente. */
data class RespuestaGet(val codigo: Int, val urlFinal: String, val tipoContenido: String, val cuerpo: String)

/** GET con límite de tamaño. Se sustituye en los tests. */
interface ClienteHttp {
    suspend fun get(url: String): RespuestaGet
}

@Singleton
class ClienteHttpUrlConnection @Inject constructor() : ClienteHttp {
    override suspend fun get(url: String): RespuestaGet = withContext(Dispatchers.IO) {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 6_000
            conexion.readTimeout = 6_000
            conexion.instanceFollowRedirects = true
            conexion.setRequestProperty("User-Agent", "EduPlanner-Android")
            conexion.setRequestProperty("Accept-Language", "es,en;q=0.8")
            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val bytes = flujo?.use { entrada ->
                val buffer = ByteArray(MAX_BYTES)
                var leidos = 0
                while (leidos < MAX_BYTES) {
                    val n = entrada.read(buffer, leidos, MAX_BYTES - leidos)
                    if (n < 0) break
                    leidos += n
                }
                buffer.copyOf(leidos)
            } ?: ByteArray(0)
            RespuestaGet(codigo, conexion.url.toString(), conexion.contentType.orEmpty(), String(bytes, Charsets.UTF_8))
        } finally {
            conexion.disconnect()
        }
    }

    private companion object {
        /** Basta para el <title> de una página o una búsqueda de libros. */
        const val MAX_BYTES = 200_000
    }
}

/**
 * Libros: se buscan en la API pública de Google Books y solo se aceptan si el título y un apellido del autor
 * coinciden; se muestran con los datos y el enlace de Google Books. Páginas: se abren y solo se aceptan si
 * responden bien, son HTML y no son una página de error; se muestran con su título real.
 */
@Singleton
class VerificadorFuentesWeb @Inject constructor(private val http: ClienteHttp) : VerificadorFuentes {

    override suspend fun verificar(propuestas: List<FuentePropuesta>): List<Fuente> = coroutineScope {
        propuestas.take(MAX_PROPUESTAS).map { propuesta ->
            async {
                withTimeoutOrNull(ESPERA_POR_FUENTE_MS) {
                    try {
                        when (propuesta.tipo) {
                            TipoFuente.LIBRO -> verificarLibro(propuesta)
                            TipoFuente.WEB -> verificarPagina(propuesta)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.i(TAG, "No se pudo comprobar «${propuesta.titulo}»: ${e.message}")
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull().distinctBy { it.url }
    }

    private suspend fun verificarLibro(propuesta: FuentePropuesta): Fuente? {
        val apellido = apellidoDe(propuesta.autor) ?: return null
        val consulta = "intitle:${propuesta.titulo} inauthor:$apellido"
        val url = "https://www.googleapis.com/books/v1/volumes?maxResults=5&printType=books&q=" +
            URLEncoder.encode(consulta, "UTF-8")
        val respuesta = http.get(url)
        if (respuesta.codigo !in 200..299) return null
        return libroCoincidente(respuesta.cuerpo, propuesta)
    }

    private suspend fun verificarPagina(propuesta: FuentePropuesta): Fuente? {
        val uri = runCatching { URI(propuesta.url.trim()) }.getOrNull() ?: return null
        if (uri.scheme != "https" || uri.host.isNullOrBlank()) return null
        val respuesta = http.get(uri.toString())
        return paginaValida(respuesta)
    }

    companion object {
        private const val TAG = "VerificadorFuentes"
        const val MAX_PROPUESTAS = 8
        const val ESPERA_POR_FUENTE_MS = 10_000L

        private val PALABRAS_DE_ERROR = listOf(
            "404", "not found", "no encontrada", "no encontrado", "no existe", "access denied",
            "forbidden", "just a moment", "attention required"
        )

        /** El apellido es la última palabra del primer autor ("Gilbert Strang" → strang). */
        internal fun apellidoDe(autor: String): String? =
            autor.split(',', ';', '&', '/').firstOrNull()
                ?.let { CatalogoModelo.normalizar(it) }
                ?.split(Regex("[^a-z]+"))?.lastOrNull { it.length >= 3 }

        /** El primer resultado de Google Books cuyo título y autor coinciden con lo propuesto. */
        internal fun libroCoincidente(json: String, propuesta: FuentePropuesta): Fuente? {
            val apellido = apellidoDe(propuesta.autor) ?: return null
            val tituloPropuesto = CatalogoModelo.normalizar(propuesta.titulo)
            val items = JSONObject(json).optJSONArray("items") ?: return null
            for (i in 0 until items.length()) {
                val info = items.optJSONObject(i)?.optJSONObject("volumeInfo") ?: continue
                val titulo = info.optString("title").trim()
                val autoresJson = info.optJSONArray("authors") ?: continue
                val autores = (0 until autoresJson.length()).map { autoresJson.optString(it) }
                val tituloReal = CatalogoModelo.normalizar(titulo)
                val tituloCoincide = tituloReal.isNotEmpty() &&
                    (tituloReal.startsWith(tituloPropuesto) || tituloPropuesto.startsWith(tituloReal))
                val autorCoincide = autores.any { apellido in CatalogoModelo.normalizar(it) }
                if (!tituloCoincide || !autorCoincide) continue
                val enlace = (info.optString("canonicalVolumeLink").ifBlank { info.optString("infoLink") })
                    .replaceFirst("http://", "https://")
                if (!enlace.startsWith("https://")) continue
                val anio = info.optString("publishedDate").take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }
                val subtitulo = info.optString("subtitle").trim()
                return Fuente(
                    TipoFuente.LIBRO,
                    if (subtitulo.isNotEmpty()) "$titulo: $subtitulo" else titulo,
                    listOfNotNull(autores.joinToString(", "), anio).joinToString(" · "),
                    enlace
                )
            }
            return null
        }

        /** Una página que abrió bien, es HTML y no es una página de error; con el título que ella misma declara. */
        internal fun paginaValida(respuesta: RespuestaGet): Fuente? {
            if (respuesta.codigo !in 200..299) return null
            if (!respuesta.urlFinal.startsWith("https://")) return null
            if (!respuesta.tipoContenido.contains("html", ignoreCase = true)) return null
            val titulo = tituloHtml(respuesta.cuerpo) ?: return null
            val normalizado = CatalogoModelo.normalizar(titulo)
            if (PALABRAS_DE_ERROR.any { normalizado.contains(it) }) return null
            val dominio = runCatching { URI(respuesta.urlFinal).host.removePrefix("www.") }.getOrNull() ?: return null
            return Fuente(TipoFuente.WEB, titulo, dominio, respuesta.urlFinal)
        }

        internal fun tituloHtml(html: String): String? =
            Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.get(1)
                ?.replace(Regex("\\s+"), " ")
                ?.replace("&amp;", "&")?.replace("&quot;", "\"")?.replace("&#39;", "'")?.replace("&#039;", "'")
                ?.replace("&lt;", "<")?.replace("&gt;", ">")?.replace("&nbsp;", " ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
    }
}
