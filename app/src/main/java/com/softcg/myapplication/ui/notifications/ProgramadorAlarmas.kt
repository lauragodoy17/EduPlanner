package com.softcg.myapplication.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.softcg.myapplication.domain.notificaciones.Aviso
import com.softcg.myapplication.domain.notificaciones.PlanAvisos
import com.softcg.myapplication.domain.notificaciones.ProgramadorAvisos
import com.softcg.myapplication.domain.notificaciones.TipoAviso
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alarmas del sistema (AlarmManager): suenan aunque la app esté cerrada o el teléfono en reposo.
 * Recuerda qué alarmas puso para quitar las que sobran al replanificar (tarea entregada, clase borrada…).
 */
@Singleton
class ProgramadorAlarmas @Inject constructor(
    @ApplicationContext private val context: Context,
    private val publicador: PublicadorAvisos
) : ProgramadorAvisos {

    private val alarmas = context.getSystemService(AlarmManager::class.java)
    private val preferencias = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    @Synchronized
    override fun programar(plan: PlanAvisos) {
        val anteriores = preferencias.getStringSet(CLAVE_ALARMAS, emptySet()).orEmpty()
        val nuevas = plan.programar.map { it.claveAlarma }.toSet()
        (anteriores - nuevas).forEach(::cancelarAlarma)
        plan.programar.forEach(::poner)
        preferencias.edit { putStringSet(CLAVE_ALARMAS, nuevas) }

        // Lo que ya está en pantalla pero dejó de tener sentido (tarea entregada, asistencia respondida…)
        publicador.visibles().filterNot { it in plan.vigentes }.forEach(publicador::retirar)
        quitarRecordatoriosAntiguos()
    }

    @Synchronized
    override fun cancelarTodo() {
        preferencias.getStringSet(CLAVE_ALARMAS, emptySet()).orEmpty().forEach(::cancelarAlarma)
        preferencias.edit { remove(CLAVE_ALARMAS) }
        publicador.visibles().forEach(publicador::retirar)
    }

    private fun poner(aviso: Aviso) {
        val intent = IntentsAvisos.alarma(context, aviso.tipo, aviso.referencia) ?: return
        val momento = aviso.momento.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Exacta para que "en 10 minutos" sea verdad; si el sistema no la permite, la inexacta llega igual, algo después
        if (exactasPermitidas()) {
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, intent)
        } else {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, intent)
        }
    }

    private fun cancelarAlarma(claveAlarma: String) {
        val tipo = TipoAviso.deClave(claveAlarma.substringBefore(':')) ?: return
        val referencia = claveAlarma.substringAfter(':')
        IntentsAvisos.alarma(context, tipo, referencia, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
            ?.let { alarmas.cancel(it); it.cancel() }
    }

    private fun exactasPermitidas() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()

    /**
     * Las versiones anteriores ponían dos recordatorios diarios repetitivos (9:00 y 11:30) con otro receptor.
     * Se quitan una vez: si no, seguirían saliendo junto al resumen nuevo.
     */
    private fun quitarRecordatoriosAntiguos() {
        if (preferencias.getBoolean(CLAVE_ANTIGUOS_QUITADOS, false)) return
        val antiguo = Intent().setClassName(context, "com.softcg.myapplication.ui.notifications.AlarmNotification")
        for (codigo in listOf(1, 2)) {
            PendingIntent.getBroadcast(context, codigo, antiguo, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
                ?.let { alarmas.cancel(it); it.cancel() }
        }
        preferencias.edit { putBoolean(CLAVE_ANTIGUOS_QUITADOS, true) }
        Log.d("ProgramadorAlarmas", "Recordatorios diarios antiguos retirados")
    }

    private companion object {
        const val ARCHIVO = "avisos_programados"
        const val CLAVE_ALARMAS = "alarmas"
        const val CLAVE_ANTIGUOS_QUITADOS = "recordatorios_antiguos_quitados"
    }
}
