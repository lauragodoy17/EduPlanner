package com.softcg.myapplication.ui.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Alarma del fin de una sesión de enfoque. El resto de avisos (clases, tareas, resumen del día, asistencia)
 * los planifica ReprogramarAvisosUseCase con ProgramadorAlarmas.
 */
object NotificationScheduler {

    private const val REQUEST_FIN_ENFOQUE = 3

    /** Avisa cuando termina la sesión de enfoque, aunque la app esté cerrada o el teléfono bloqueado. */
    fun programarFinEnfoque(context: Context, finMillis: Long) {
        CanalesNotificacion.crear(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Tiene que ser exacta: la inexacta puede retrasarse más de 10 minutos, demasiado para un temporizador
        val exactaPermitida = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exactaPermitida) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finMillis, intentFinEnfoque(context))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finMillis, intentFinEnfoque(context))
        }
    }

    /** Quita también la notificación: ya no hay nada que avisar si la sesión se cobró o se abandonó. */
    fun cancelarFinEnfoque(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(intentFinEnfoque(context))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(EnfoqueNotification.NOTIFICATION_ID)
    }

    private fun intentFinEnfoque(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context.applicationContext,
        REQUEST_FIN_ENFOQUE,
        Intent(context.applicationContext, EnfoqueNotification::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
