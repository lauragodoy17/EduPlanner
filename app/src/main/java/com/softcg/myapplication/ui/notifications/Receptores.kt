package com.softcg.myapplication.ui.notifications

import android.content.Context
import android.content.Intent
import android.util.Log
import com.softcg.myapplication.domain.notificaciones.CanalAviso
import com.softcg.myapplication.domain.notificaciones.ConstruirAvisoUseCase
import com.softcg.myapplication.domain.notificaciones.ReprogramarAvisosUseCase
import com.softcg.myapplication.domain.notificaciones.ResponderAvisoUseCase
import com.softcg.myapplication.domain.notificaciones.TipoAccionAviso
import com.softcg.myapplication.domain.notificaciones.TipoAviso
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "Avisos"

/** Suena una alarma: se arma el aviso con los datos de ese momento, se publica y se planifican los siguientes. */
@AndroidEntryPoint
class AvisoReceiver : ReceptorConHilt() {

    @Inject lateinit var construir: ConstruirAvisoUseCase
    @Inject lateinit var publicador: PublicadorAvisos
    @Inject lateinit var reprogramar: ReprogramarAvisosUseCase

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val tipo = TipoAviso.deClave(intent.getStringExtra(IntentsAvisos.EXTRA_TIPO)) ?: return
        val referencia = intent.getStringExtra(IntentsAvisos.EXTRA_REFERENCIA) ?: return
        enSegundoPlano {
            runCatching { construir(tipo, referencia)?.let(publicador::publicar) }
                .onFailure { Log.w(TAG, "No se pudo mostrar el aviso $tipo:$referencia", it) }
            runCatching { reprogramar() }.onFailure { Log.w(TAG, "No se pudieron replanificar los avisos", it) }
        }
    }
}

/** Botones de la notificación: registrar la asistencia o entregar la tarea sin abrir la app. */
@AndroidEntryPoint
class AccionAvisoReceiver : ReceptorConHilt() {

    @Inject lateinit var responder: ResponderAvisoUseCase
    @Inject lateinit var publicador: PublicadorAvisos
    @Inject lateinit var reprogramar: ReprogramarAvisosUseCase

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val accion = intent.getStringExtra(IntentsAvisos.EXTRA_ACCION)
            ?.let { nombre -> TipoAccionAviso.entries.firstOrNull { it.name == nombre } } ?: return
        val referencia = intent.getStringExtra(IntentsAvisos.EXTRA_REFERENCIA) ?: return
        val clave = intent.getStringExtra(IntentsAvisos.EXTRA_CLAVE_NOTIFICACION) ?: return
        val canal = if (accion == TipoAccionAviso.ENTREGAR) CanalAviso.TAREAS else CanalAviso.ASISTENCIA
        enSegundoPlano {
            runCatching { publicador.mostrarRespuesta(clave, canal, responder(accion, referencia)) }
                .onFailure { Log.w(TAG, "No se pudo responder $accion:$referencia", it) }
            runCatching { reprogramar() }.onFailure { Log.w(TAG, "No se pudieron replanificar los avisos", it) }
        }
    }
}

/**
 * Las alarmas se borran al reiniciar el teléfono y pueden quedar mal tras cambiar la hora o actualizar la app:
 * en esos casos se vuelven a planificar.
 */
@AndroidEntryPoint
class ReprogramarAvisosReceiver : ReceptorConHilt() {

    @Inject lateinit var reprogramar: ReprogramarAvisosUseCase

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action !in ACCIONES) return
        enSegundoPlano {
            runCatching { reprogramar() }.onFailure { Log.w(TAG, "No se pudieron replanificar tras ${intent.action}", it) }
        }
    }

    private companion object {
        val ACCIONES = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
    }
}
