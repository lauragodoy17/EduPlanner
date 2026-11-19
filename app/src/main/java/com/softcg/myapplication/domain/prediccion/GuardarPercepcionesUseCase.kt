package com.softcg.myapplication.domain.prediccion

import android.util.Log
import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.PercepcionesRepository
import com.softcg.myapplication.data.network.SesionUsuario
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Lo que el estudiante declara de una asignatura en las dos hojas de "Datos para predicciones":
 * - [estudio]: horas por semana .
 * - [interesYDificultad]: interés y dificultad percibida (una sola hoja, dos preguntas).
 * Lo que se deja sin responder no se guarda. Interés y horas se guardan una vez por mes (responder de nuevo
 * ese mes corrige la respuesta).
 */
class GuardarPercepcionesUseCase @Inject constructor(
    private val asignaturas: AsignaturasRepository,
    private val percepciones: PercepcionesRepository,
    private val gamificacion: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    private companion object {
        val MES: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }

    suspend fun estudio(asignaturaId: Int, horasSemana: Float?, metodo: String?): ResultadoGuardarDatos {
        if (horasSemana == null && metodo == null) return ResultadoGuardarDatos.Invalido("Responde al menos una de las dos preguntas")
        if (horasSemana != null && horasSemana !in 0f..ReglasExtraccion.MAX_HORAS_SEMANA) {
            return ResultadoGuardarDatos.Invalido("Indica entre 0 y ${ReglasExtraccion.MAX_HORAS_SEMANA.toInt()} horas por semana")
        }
        if (metodo != null && !CatalogoModelo.esMetodo(metodo)) return ResultadoGuardarDatos.Invalido("Elige un método de la lista")

        return guardar(asignaturaId) { millis ->
            val ahora = reloj.ahora()
            horasSemana?.let {
                percepciones.guardar(ReglasExtraccion.TIPO_HORAS, "HORAS:$asignaturaId:${ahora.format(MES)}", asignaturaId, null, it, null, millis)
            }
            metodo?.let {
                percepciones.guardar(ReglasExtraccion.TIPO_METODO, "METODO:$asignaturaId:${ahora.toLocalDate()}", asignaturaId, null, null, it, millis)
            }
        }
    }

    /** [tema] es el de la tarea cuando se responde al predecirla; null para la asignatura en general. */
    suspend fun interesYDificultad(asignaturaId: Int, tema: String?, interes: Float?, dificultad: Float?): ResultadoGuardarDatos {
        if (interes == null && dificultad == null) return ResultadoGuardarDatos.Invalido("Responde al menos una de las dos preguntas")
        if (interes != null && interes !in 0f..10f) return ResultadoGuardarDatos.Invalido("Interés fuera de rango")
        if (dificultad != null && dificultad !in 0f..10f) return ResultadoGuardarDatos.Invalido("Dificultad fuera de rango")

        return guardar(asignaturaId) { millis ->
            val ahora = reloj.ahora()
            interes?.let {
                percepciones.guardar(ReglasExtraccion.TIPO_INTERES, "INTERES:$asignaturaId:${ahora.format(MES)}", asignaturaId, null, it, null, millis)
            }
            dificultad?.let {
                // Misma clave que GuardarDificultadUseCase.deAsignatura: responder otra vez el mismo día la corrige
                val origen = "DECLARADA:$asignaturaId:${tema ?: "-"}:${ahora.toLocalDate()}"
                percepciones.guardar(ReglasExtraccion.TIPO_DIFICULTAD, origen, asignaturaId, tema, it, null, millis)
            }
        }
    }

    private suspend fun guardar(asignaturaId: Int, escribir: suspend (millis: Long) -> Unit): ResultadoGuardarDatos {
        sesion.uid() ?: return ResultadoGuardarDatos.SinSesion
        return try {
            gamificacion.enTransaccion {
                asignaturas.getAsignatura(asignaturaId)
                    ?: return@enTransaccion ResultadoGuardarDatos.Invalido("La asignatura ya no existe")
                escribir(reloj.ahora().toInstant().toEpochMilli())
                ResultadoGuardarDatos.Guardado
            }
        } catch (e: Exception) {
            Log.w("GuardarPercepciones", "No se pudo guardar la respuesta", e)
            ResultadoGuardarDatos.Error
        }
    }
}
