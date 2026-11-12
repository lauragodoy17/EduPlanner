package com.softcg.myapplication.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cliente de la API de modelos predictivos (proyecto modelos_exportados, desplegado en la VM).
 * Cada petición lleva el ID token de Firebase del usuario con sesión: la API lo exige.
 */
@Singleton
class PrediccionService @Inject constructor(private val firebase: FirebaseClient) {

    suspend fun predecir(cuerpoJson: String, topN: Int): RespuestaApi {
        val usuario = firebase.auth.currentUser ?: return RespuestaApi.SinSesion
        // false: reutiliza el token guardado y solo pide uno nuevo si ya venció (duran una hora)
        val token = runCatching { usuario.getIdToken(false).await().token }.getOrNull()
            ?: return RespuestaApi.ErrorRed("No se pudo obtener el token de Firebase")
        return withContext(Dispatchers.IO) {
            runCatching { enviar("$URL_BASE/predict?top_n=$topN", token, cuerpoJson) }
                .getOrElse { RespuestaApi.ErrorRed(it.message ?: it.javaClass.simpleName) }
        }
    }

    private fun enviar(url: String, token: String, cuerpo: String): RespuestaApi {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "POST"
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 30_000
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conexion.setRequestProperty("Accept", "application/json")
            conexion.setRequestProperty("Authorization", "Bearer $token")
            conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }

            val codigo = conexion.responseCode
            // Con 4xx/5xx el cuerpo (p. ej. {"detail": ...}) viene por errorStream
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return RespuestaApi.Http(codigo, texto)
        } finally {
            conexion.disconnect()
        }
    }

    companion object {
        const val URL_BASE = "https://softcgeduplanner.duckdns.org"
        const val RUTA_PREDICT = "/predict"
    }
}

sealed class RespuestaApi {
    /** La API respondió, con éxito o con error (401 token, 422 datos inválidos, 429 límite...). */
    data class Http(val codigo: Int, val cuerpo: String) : RespuestaApi() {
        val exitosa: Boolean get() = codigo in 200..299
    }
    object SinSesion : RespuestaApi()
    /** No hubo respuesta: sin internet, DNS, timeout, certificado... */
    data class ErrorRed(val mensaje: String) : RespuestaApi()
}
