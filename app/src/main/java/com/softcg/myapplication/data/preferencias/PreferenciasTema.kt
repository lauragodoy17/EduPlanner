package com.softcg.myapplication.data.preferencias

import android.content.Context
import androidx.core.content.edit
import com.softcg.myapplication.domain.preferencias.TemaApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tema elegido en este dispositivo. Se lee de forma síncrona porque hay que aplicarlo antes de crear la
 * pantalla (antes de que Hilt inyecte nada), por eso también se puede construir a mano con el contexto.
 */
@Singleton
class PreferenciasTema @Inject constructor(@ApplicationContext context: Context) {

    private val preferencias = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun tema(): TemaApp = TemaApp.deClave(preferencias.getString(CLAVE_TEMA, null))

    fun guardar(tema: TemaApp) {
        preferencias.edit { putString(CLAVE_TEMA, tema.clave) }
    }

    private companion object {
        const val ARCHIVO = "preferencias_app"
        const val CLAVE_TEMA = "tema"
    }
}
