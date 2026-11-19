package com.softcg.myapplication.core.ex

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/** Lado mayor de la copia de una imagen de evento: de sobra para la pantalla, y una foto de 50 MP ya no pesa 200 MB. */
private const val LADO_MAXIMO = 1600

/**
 * Copia la imagen elegida para un evento a los archivos de la app, reducida y ya girada según su orientación.
 * La Uri que da el selector solo vale mientras la app sigue abierta (después de reiniciar ya no se podía leer) y una
 * foto muy grande cerraba la app al dibujarla. Devuelve la Uri de la copia, o null si la imagen no se pudo leer.
 */
suspend fun Context.copiarImagenEvento(origen: Uri): Uri? = withContext(Dispatchers.IO) {
    try {
        val fuente = ImageDecoder.createSource(contentResolver, origen)
        val bitmap = ImageDecoder.decodeBitmap(fuente) { decodificador, info, _ ->
            val lado = max(info.size.width, info.size.height)
            if (lado > LADO_MAXIMO) {
                val escala = LADO_MAXIMO.toFloat() / lado
                decodificador.setTargetSize(
                    (info.size.width * escala).toInt().coerceAtLeast(1),
                    (info.size.height * escala).toInt().coerceAtLeast(1)
                )
            }
            // Software: se puede comprimir a JPEG
            decodificador.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val carpeta = File(filesDir, "imagenes_eventos").apply { mkdirs() }
        val destino = File(carpeta, "${UUID.randomUUID()}.jpg")
        destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        Uri.fromFile(destino)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("ImagenEvento", "No se pudo copiar la imagen $origen", e)
        null
    }
}
