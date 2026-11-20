package com.softcg.myapplication.data.preferencias

import android.content.Context
import androidx.core.content.edit
import com.softcg.myapplication.domain.encuesta.AlmacenEncuesta
import com.softcg.myapplication.domain.encuesta.RespuestasEncuesta
import com.softcg.myapplication.domain.encuesta.nuevoId
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** La encuesta en este dispositivo: el borrador sobrevive si se sale de la pantalla o se cierra la app. */
@Singleton
class PreferenciasEncuesta @Inject constructor(@ApplicationContext context: Context) : AlmacenEncuesta {

    private val preferencias = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun borrador(): RespuestasEncuesta = runCatching {
        val json = JSONObject(preferencias.getString(CLAVE_BORRADOR, null) ?: return emptyMap())
        json.keys().asSequence().associateWith { json.optString(it) }
    }.getOrDefault(emptyMap())

    override fun guardarBorrador(respuestas: RespuestasEncuesta) {
        preferencias.edit { putString(CLAVE_BORRADOR, JSONObject(respuestas).toString()) }
    }

    override fun seccion(): Int = preferencias.getInt(CLAVE_SECCION, 0)

    override fun guardarSeccion(seccion: Int) {
        preferencias.edit { putInt(CLAVE_SECCION, seccion) }
    }

    override fun enviada(): Boolean = preferencias.getBoolean(CLAVE_ENVIADA, false)

    /** Ya enviada no hace falta el borrador ni su id. */
    override fun marcarEnviada() {
        preferencias.edit {
            putBoolean(CLAVE_ENVIADA, true)
            remove(CLAVE_BORRADOR)
            remove(CLAVE_SECCION)
            remove(CLAVE_ID_RESPUESTA)
        }
    }

    override fun participante(): String = idFijo(CLAVE_PARTICIPANTE)

    override fun idRespuesta(): String = idFijo(CLAVE_ID_RESPUESTA)

    private fun idFijo(clave: String): String =
        preferencias.getString(clave, null) ?: nuevoId().also { preferencias.edit { putString(clave, it) } }

    private companion object {
        const val ARCHIVO = "encuesta"
        const val CLAVE_BORRADOR = "borrador"
        const val CLAVE_SECCION = "seccion"
        const val CLAVE_ENVIADA = "enviada"
        const val CLAVE_PARTICIPANTE = "participante"
        const val CLAVE_ID_RESPUESTA = "id_respuesta"
    }
}
