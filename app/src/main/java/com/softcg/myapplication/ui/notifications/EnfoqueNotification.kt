package com.softcg.myapplication.ui.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.InicioActivity

/** Avisa de que terminó una sesión de enfoque. La recompensa se cobra al abrir la sesión en la app. */
class EnfoqueNotification : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID = 2
        const val EXTRA_ABRIR_ENFOQUE = "abrir_enfoque"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val abrir = Intent(context, InicioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ABRIR_ENFOQUE, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.MY_CHANNEL_ID)
            .setSmallIcon(R.drawable.logoblanco)
            .setContentTitle("Sesión de enfoque terminada")
            .setContentText("Buen trabajo. Abre EduPlanner para cobrar tu recompensa.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
