package com.softcg.myapplication.ui.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationScheduler {

    const val MY_CHANNEL_ID = "myChannel"

    private const val REQUEST_MANANA = 1
    private const val REQUEST_TARDE = 2
    private const val REQUEST_FIN_ENFOQUE = 3

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            MY_CHANNEL_ID,
            "MySuperChannel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "SUSCRIBETE"
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /** Programa los recordatorios diarios de las 9:00 y las 11:30. */
    fun scheduleDailyReminders(context: Context, pendientes: String) {
        scheduleDaily(context, REQUEST_MANANA, 9, 0, pendientes)
        scheduleDaily(context, REQUEST_TARDE, 11, 30, pendientes)
    }

    /** Avisa cuando termina la sesión de enfoque, aunque la app esté cerrada o el teléfono bloqueado. */
    fun programarFinEnfoque(context: Context, finMillis: Long) {
        createChannel(context)
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

    /** Próxima fecha a la [hour]:[minute]; si esa hora ya pasó hoy, la de mañana. */
    fun nextTriggerTime(now: Calendar, hour: Int, minute: Int): Calendar =
        (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

    private fun scheduleDaily(context: Context, requestCode: Int, hour: Int, minute: Int, pendientes: String) {
        val intent = Intent(context.applicationContext, AlarmNotification::class.java)
        intent.putExtra("Pendientes", pendientes)
        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerTime(Calendar.getInstance(), hour, minute).timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
