package com.softcg.myapplication.ui.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.softcg.myapplication.domain.notificaciones.DestinoAviso
import com.softcg.myapplication.domain.notificaciones.TipoAccionAviso
import com.softcg.myapplication.domain.notificaciones.TipoAviso
import com.softcg.myapplication.ui.Inicio.InicioActivity

/**
 * Los intents de las alarmas, de los botones y de abrir la app desde una notificación.
 * Cada uno lleva un `data` propio: así Android distingue los PendingIntent sin depender del requestCode.
 */
object IntentsAvisos {

    const val EXTRA_TIPO = "aviso_tipo"
    const val EXTRA_REFERENCIA = "aviso_referencia"
    const val EXTRA_ACCION = "aviso_accion"
    const val EXTRA_CLAVE_NOTIFICACION = "aviso_clave_notificacion"

    /** Destino al tocar una notificación; lo lee InicioActivity. */
    const val EXTRA_DESTINO = "aviso_destino"
    const val EXTRA_TAREA_ID = "aviso_tarea_id"
    const val EXTRA_TAREA_TITULO = "aviso_tarea_titulo"
    const val EXTRA_TAREA_ASIGNATURA = "aviso_tarea_asignatura"
    const val DESTINO_TAREA = "tarea"
    const val DESTINO_HORARIO = "horario"
    const val DESTINO_ASISTENCIA = "asistencia"
    const val DESTINO_INICIO = "inicio"

    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun alarma(context: Context, tipo: TipoAviso, referencia: String, flags: Int = FLAGS): PendingIntent? {
        val intent = Intent(context, AvisoReceiver::class.java)
            .setData(Uri.parse("eduplanner://aviso/${tipo.clave}/${Uri.encode(referencia)}"))
            .putExtra(EXTRA_TIPO, tipo.clave)
            .putExtra(EXTRA_REFERENCIA, referencia)
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun accion(context: Context, accion: TipoAccionAviso, referencia: String, claveNotificacion: String): PendingIntent {
        val intent = Intent(context, AccionAvisoReceiver::class.java)
            .setData(Uri.parse("eduplanner://accion/${accion.name}/${Uri.encode(referencia)}"))
            .putExtra(EXTRA_ACCION, accion.name)
            .putExtra(EXTRA_REFERENCIA, referencia)
            .putExtra(EXTRA_CLAVE_NOTIFICACION, claveNotificacion)
        return PendingIntent.getBroadcast(context, 0, intent, FLAGS)
    }

    fun abrir(context: Context, destino: DestinoAviso, claveNotificacion: String): PendingIntent {
        val intent = Intent(context, InicioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.parse("eduplanner://abrir/${Uri.encode(claveNotificacion)}")
            when (destino) {
                is DestinoAviso.AbrirTarea -> {
                    putExtra(EXTRA_DESTINO, DESTINO_TAREA)
                    putExtra(EXTRA_TAREA_ID, destino.tareaId)
                    putExtra(EXTRA_TAREA_TITULO, destino.titulo)
                    putExtra(EXTRA_TAREA_ASIGNATURA, destino.asignatura)
                }
                DestinoAviso.Horario -> putExtra(EXTRA_DESTINO, DESTINO_HORARIO)
                DestinoAviso.ConfirmarAsistencia -> putExtra(EXTRA_DESTINO, DESTINO_ASISTENCIA)
                DestinoAviso.Inicio -> putExtra(EXTRA_DESTINO, DESTINO_INICIO)
            }
        }
        return PendingIntent.getActivity(context, 0, intent, FLAGS)
    }
}
