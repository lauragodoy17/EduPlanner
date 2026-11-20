package com.softcg.myapplication.data.network

import android.util.Log
import com.softcg.myapplication.BuildConfig
import com.softcg.myapplication.domain.encuesta.ColumnaEncuesta
import com.softcg.myapplication.domain.encuesta.EnviadorEncuesta
import com.softcg.myapplication.domain.encuesta.ResultadoEnvioEncuesta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Envía la respuesta al Google Apps Script publicado como aplicación web sobre la hoja de cálculo (código en
 * `encuesta/Codigo.gs`). La dirección y la clave salen de local.properties (`encuesta.url`, `encuesta.clave`).
 * Apps Script responde con una redirección a googleusercontent.com, que HttpURLConnection sigue (como GET).
 */
@Singleton
class EncuestaSheetsService @Inject constructor() : EnviadorEncuesta {

    override suspend fun enviar(idRespuesta: String, columnas: List<ColumnaEncuesta>): ResultadoEnvioEncuesta {
        val url = BuildConfig.ENCUESTA_URL
        if (url.isBlank()) return ResultadoEnvioEncuesta.NoConfigurada
        val cuerpo = JSONObject()
            .put("clave", BuildConfig.ENCUESTA_CLAVE)
            .put("id_respuesta", idRespuesta)
            .put("columnas", JSONArray(columnas.map { JSONObject().put("encabezado", it.encabezado).put("valor", it.valor ?: "") }))
            .toString()
        return withContext(Dispatchers.IO) {
            try {
                val conexion = URL(url).openConnection() as HttpURLConnection
                try {
                    conexion.requestMethod = "POST"
                    conexion.connectTimeout = 15_000
                    conexion.readTimeout = 30_000
                    conexion.doOutput = true
                    conexion.instanceFollowRedirects = true
                    conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
                    val codigo = conexion.responseCode
                    val texto = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    interpretar(codigo, texto)
                } finally {
                    conexion.disconnect()
                }
            } catch (e: IOException) {
                Log.w(TAG, "Sin conexión al enviar la encuesta", e)
                ResultadoEnvioEncuesta.SinConexion
            }
        }
    }

    /** El script contesta {"ok": true} o {"ok": false, "error": "..."}; una página HTML es un despliegue mal configurado. */
    internal fun interpretar(codigo: Int, texto: String): ResultadoEnvioEncuesta {
        val json = runCatching { JSONObject(texto) }.getOrNull()
        return when {
            codigo in 200..299 && json?.optBoolean("ok") == true -> ResultadoEnvioEncuesta.Enviada
            json != null -> ResultadoEnvioEncuesta.Error(json.optString("error", "respuesta inesperada")).also { Log.w(TAG, "La hoja rechazó la encuesta: $texto") }
            else -> ResultadoEnvioEncuesta.Error("HTTP $codigo").also { Log.w(TAG, "Respuesta no JSON ($codigo): ${texto.take(300)}") }
        }
    }

    private companion object {
        const val TAG = "EncuestaSheets"
    }
}
