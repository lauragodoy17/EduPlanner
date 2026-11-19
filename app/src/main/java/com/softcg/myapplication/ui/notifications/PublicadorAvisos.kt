package com.softcg.myapplication.ui.notifications

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.notificaciones.CanalAviso
import com.softcg.myapplication.domain.notificaciones.ContenidoAviso
import com.softcg.myapplication.domain.notificaciones.DestinoAviso
import com.softcg.myapplication.domain.notificaciones.RespuestaAccion
import com.softcg.myapplication.domain.notificaciones.TonoAviso
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convierte un [ContenidoAviso] en una notificación del sistema: icono de color según la urgencia, texto
 * desplegable, botones y la pantalla a la que lleva. Una notificación por tarea o clase (etiqueta = su clave):
 * cada aviso nuevo reemplaza al anterior en vez de amontonarse.
 */
@Singleton
class PublicadorAvisos @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /** Sin permiso (Android 13+) o con las notificaciones apagadas no se intenta publicar. */
    fun puedeNotificar(): Boolean = manager.areNotificationsEnabled()

    @SuppressLint("MissingPermission") // puedeNotificar() comprueba el permiso
    fun publicar(contenido: ContenidoAviso) {
        if (!puedeNotificar()) return
        CanalesNotificacion.crear(context)
        val color = context.getColor(colorDe(contenido.tono))
        val builder = NotificationCompat.Builder(context, CanalesNotificacion.id(contenido.canal))
            .setSmallIcon(iconoDe(contenido.canal))
            .setLargeIcon(iconoGrande(iconoDe(contenido.canal), color))
            .setColor(color)
            .setContentTitle(contenido.titulo)
            .setContentText(contenido.texto)
            .setCategory(if (contenido.canal == CanalAviso.CLASES) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (contenido.canal == CanalAviso.RESUMEN) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(IntentsAvisos.abrir(context, contenido.destino, contenido.claveNotificacion))

        if (contenido.lineas.isNotEmpty()) {
            builder.setStyle(NotificationCompat.InboxStyle().also { estilo ->
                contenido.lineas.forEach(estilo::addLine)
                estilo.setBigContentTitle(contenido.titulo)
            })
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contenido.detalle ?: contenido.texto))
        }

        // La hora que importa (inicio de la clase, límite de la tarea) ya va en el texto: sin "hace 5 min" que le quite sitio al título
        builder.setShowWhen(false)
        contenido.acciones.forEach { accion ->
            builder.addAction(
                0, accion.etiqueta,
                IntentsAvisos.accion(context, accion.tipo, contenido.referencia, contenido.claveNotificacion)
            )
        }
        manager.notify(etiqueta(contenido.claveNotificacion), ID_AVISO, builder.build())
    }

    /**
     * Tras pulsar un botón: la pregunta se sustituye por el resultado, que se retira solo a los pocos segundos.
     * Lleva otra etiqueta para que la replanificación (que retira lo que dejó de tener sentido) no la quite al instante.
     */
    @SuppressLint("MissingPermission")
    fun mostrarRespuesta(claveNotificacion: String, canal: CanalAviso, respuesta: RespuestaAccion) {
        if (!puedeNotificar()) return
        val color = context.getColor(if (respuesta.exito) R.color.aviso_exito else R.color.aviso_atencion)
        val notificacion = NotificationCompat.Builder(context, CanalesNotificacion.id(canal))
            .setSmallIcon(R.drawable.ic_gami_check)
            .setLargeIcon(iconoGrande(if (respuesta.exito) R.drawable.ic_gami_check else R.drawable.ic_gami_tarea, color))
            .setColor(color)
            .setContentTitle(respuesta.titulo)
            .setContentText(respuesta.texto)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setTimeoutAfter(DURACION_RESPUESTA_MS)
            .setContentIntent(IntentsAvisos.abrir(context, DestinoAviso.Inicio, claveNotificacion))
            .build()
        retirar(claveNotificacion)
        manager.notify(PREFIJO_RESPUESTA + claveNotificacion, ID_AVISO, notificacion)
    }

    fun retirar(claveNotificacion: String) = manager.cancel(etiqueta(claveNotificacion), ID_AVISO)

    /** Claves de los avisos que se están viendo ahora mismo. */
    fun visibles(): List<String> = manager.activeNotifications
        .mapNotNull { it.tag?.takeIf { tag -> it.id == ID_AVISO && tag.startsWith(PREFIJO) }?.removePrefix(PREFIJO) }

    @ColorRes
    private fun colorDe(tono: TonoAviso) = when (tono) {
        TonoAviso.MARCA -> R.color.aviso_marca
        TonoAviso.EXITO -> R.color.aviso_exito
        TonoAviso.ATENCION -> R.color.aviso_atencion
        TonoAviso.URGENTE -> R.color.aviso_urgente
    }

    @DrawableRes
    private fun iconoDe(canal: CanalAviso) = when (canal) {
        CanalAviso.CLASES -> R.drawable.ic_gami_clase
        CanalAviso.TAREAS -> R.drawable.ic_gami_tarea
        CanalAviso.RESUMEN -> R.drawable.ic_gami_estrella
        CanalAviso.ASISTENCIA -> R.drawable.ic_gami_check
    }

    /** Círculo del color del aviso con el icono en blanco: se distingue de un vistazo en la barra de notificaciones. */
    private fun iconoGrande(@DrawableRes icono: Int, color: Int): Bitmap {
        val lado = (64 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawCircle(lado / 2f, lado / 2f, lado / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        ContextCompat.getDrawable(context, icono)?.mutate()?.let { dibujo ->
            val margen = lado / 4
            dibujo.setBounds(margen, margen, lado - margen, lado - margen)
            dibujo.setTint(android.graphics.Color.WHITE)
            dibujo.draw(canvas)
        }
        return bitmap
    }

    private fun etiqueta(claveNotificacion: String) = PREFIJO + claveNotificacion

    private companion object {
        const val PREFIJO = "aviso|"
        const val PREFIJO_RESPUESTA = "respuesta|"
        const val ID_AVISO = 100
        const val DURACION_RESPUESTA_MS = 6_000L
    }
}
