package com.softcg.myapplication.ui.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.InicioActivity

class AlarmNotification: BroadcastReceiver() {
    companion object{
        const val NOTIFICATION_ID = 1
    }
    var pendientes:String="0"
    override fun onReceive(context: Context, p1: Intent?) {
        if (p1 != null) {
            pendientes=p1.getStringExtra("Pendientes").toString()
        }
        createSimpleNotification(context)
    }

    private fun createSimpleNotification(context: Context) {
        val intent = Intent(context, InicioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val flag = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, flag)

        val notification = NotificationCompat.Builder(context, InicioActivity.MY_CHANNEL_ID)
            .setSmallIcon(R.drawable.logoblanco)
            .setContentTitle("No hay tiempo para descanso")
            .setContentText("Aún te quedan ${pendientes} Tareas por hacer!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A TRABAJAR!")
            )
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val notificationc = NotificationCompat.Builder(context, InicioActivity.MY_CHANNEL_ID)
            .setSmallIcon(R.drawable.logoblanco)
            .setContentTitle("Bien Hecho")
            .setContentText("No tienes tareas pendientes!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("A DESCANSAR!")
            )
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (pendientes=="0"){
            manager.notify(NOTIFICATION_ID, notificationc)
        }else{
            manager.notify(NOTIFICATION_ID, notification)
        }
    }
}