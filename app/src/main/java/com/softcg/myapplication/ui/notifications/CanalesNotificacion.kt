package com.softcg.myapplication.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.softcg.myapplication.domain.notificaciones.CanalAviso

/**
 * Canales del sistema: el estudiante puede silenciar cada tipo de aviso por separado en los ajustes del teléfono.
 * Los ids no se cambian: Android recuerda la configuración que el estudiante eligió para cada uno.
 */
object CanalesNotificacion {

    /** Fin de las sesiones de enfoque (id histórico de la app, se conserva por lo mismo). */
    const val ENFOQUE = "myChannel"
    private const val CLASES = "clases"
    private const val TAREAS = "tareas"
    private const val RESUMEN = "resumen_dia"
    private const val ASISTENCIA = "asistencia"

    fun id(canal: CanalAviso): String = when (canal) {
        CanalAviso.CLASES -> CLASES
        CanalAviso.TAREAS -> TAREAS
        CanalAviso.RESUMEN -> RESUMEN
        CanalAviso.ASISTENCIA -> ASISTENCIA
    }

    /** Idempotente: crearlos de nuevo solo actualiza nombre y descripción. */
    fun crear(context: Context) {
        val canales = listOf(
            NotificationChannel(CLASES, "Clases", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Te avisa 10 minutos antes de cada clase"
            },
            NotificationChannel(TAREAS, "Entregas de tareas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "1 hora y 5 minutos antes de que venza una tarea, y cuando se vence"
            },
            NotificationChannel(ASISTENCIA, "Asistencia a clase", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Al terminar una clase, para registrar si asististe"
            },
            NotificationChannel(RESUMEN, "Resumen del día", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A las 9:00, las tareas y eventos de ese día"
            },
            NotificationChannel(ENFOQUE, "Sesiones de enfoque", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cuando termina una sesión de enfoque"
            }
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(canales)
    }
}
