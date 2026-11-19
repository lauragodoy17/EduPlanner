package com.softcg.myapplication.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base de los receptores con @AndroidEntryPoint: Hilt inyecta dentro de onReceive y exige llamar a
 * super.onReceive(), que en BroadcastReceiver es abstracto.
 */
abstract class ReceptorConHilt : BroadcastReceiver() {
    @CallSuper
    override fun onReceive(context: Context, intent: Intent) {}

    /** Trabajo corto fuera del hilo principal; el sistema mantiene vivo el proceso hasta que termina. */
    protected fun enSegundoPlano(bloque: suspend () -> Unit) {
        val pendiente = goAsync()
        alcance.launch {
            try {
                bloque()
            } finally {
                pendiente.finish()
            }
        }
    }

    private companion object {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
