package com.softcg.myapplication.domain.encuesta

import com.softcg.myapplication.core.Reloj
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

sealed class ResultadoEnvioEncuesta {
    object Enviada : ResultadoEnvioEncuesta()
    /** La app no tiene la dirección de la hoja (falta `encuesta.url` en local.properties). */
    object NoConfigurada : ResultadoEnvioEncuesta()
    object SinConexion : ResultadoEnvioEncuesta()
    data class Error(val motivo: String) : ResultadoEnvioEncuesta()
    /** Hay preguntas sin responder; [seccion] es el índice de la primera sección incompleta. */
    data class Incompleta(val seccion: Int) : ResultadoEnvioEncuesta()
}

/** Escribe la fila en la hoja de Google Sheets. Se sustituye en los tests. */
interface EnviadorEncuesta {
    suspend fun enviar(idRespuesta: String, columnas: List<ColumnaEncuesta>): ResultadoEnvioEncuesta
}

/** Borrador, identificadores y si ya se respondió, en este dispositivo. */
interface AlmacenEncuesta {
    fun borrador(): RespuestasEncuesta
    fun guardarBorrador(respuestas: RespuestasEncuesta)
    fun seccion(): Int
    fun guardarSeccion(seccion: Int)
    fun enviada(): Boolean
    fun marcarEnviada()
    /** Anónimo y fijo por instalación: permite ver si alguien respondió dos veces sin saber quién es. */
    fun participante(): String
    /**
     * El mismo id para todos los reintentos de un borrador: si la hoja ya guardó la fila pero la respuesta no
     * llegó, el reintento no la duplica.
     */
    fun idRespuesta(): String
}

class EnviarEncuestaUseCase @Inject constructor(
    private val enviador: EnviadorEncuesta,
    private val almacen: AlmacenEncuesta,
    private val reloj: Reloj,
    private val version: VersionApp
) {
    suspend operator fun invoke(respuestas: RespuestasEncuesta): ResultadoEnvioEncuesta {
        ReglasEncuesta.primeraIncompleta(respuestas)?.let { return ResultadoEnvioEncuesta.Incompleta(it) }
        val meta = MetaEncuesta(
            fecha = reloj.ahora().format(FORMATO_FECHA),
            idRespuesta = almacen.idRespuesta(),
            participante = almacen.participante(),
            version = version.nombre
        )
        val resultado = enviador.enviar(meta.idRespuesta, ReglasEncuesta.columnas(respuestas, meta))
        if (resultado == ResultadoEnvioEncuesta.Enviada) almacen.marcarEnviada()
        return resultado
    }

    private companion object {
        val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}

/** Versión de la app que respondió (para separar respuestas de versiones distintas). */
data class VersionApp(val nombre: String)

/** Ids aleatorios, separados para poder fijarlos en los tests. */
fun nuevoId(): String = UUID.randomUUID().toString()
