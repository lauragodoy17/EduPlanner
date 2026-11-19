package com.softcg.myapplication.data.preferencias

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Si ya se explicó y pidió el permiso de notificaciones: no se insiste en cada apertura. */
@Singleton
class PreferenciasAvisos @Inject constructor(@ApplicationContext context: Context) {

    private val preferencias = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    var permisoPedido: Boolean
        get() = preferencias.getBoolean(CLAVE_PERMISO_PEDIDO, false)
        set(valor) = preferencias.edit { putBoolean(CLAVE_PERMISO_PEDIDO, valor) }

    private companion object {
        const val ARCHIVO = "preferencias_app"
        const val CLAVE_PERMISO_PEDIDO = "permiso_notificaciones_pedido"
    }
}
