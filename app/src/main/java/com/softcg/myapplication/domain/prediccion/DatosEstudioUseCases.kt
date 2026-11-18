package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.PercepcionesRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Escalas de 5 respuestas para lo que solo el estudiante sabe. Se guardan en 0-10, la escala del modelo.
 * Cinco opciones con nombre son más fáciles de contestar con honestidad que un número del 0 al 10.
 */
object NivelesPercepcion {
    val INTERES = listOf("Nada", "Poco", "Algo", "Bastante", "Mucho")
    val DIFICULTAD = listOf("Muy fácil", "Fácil", "Normal", "Difícil", "Muy difícil")

    fun valor(indice: Int): Float = indice * 2.5f
    fun indice(valor: Float): Int = (valor / 2.5f).toInt().coerceIn(0, 4)
}

sealed class ResultadoGuardarDatos {
    object Guardado : ResultadoGuardarDatos()
    data class Invalido(val motivo: String) : ResultadoGuardarDatos()
    object SinSesion : ResultadoGuardarDatos()
    object Error : ResultadoGuardarDatos()
}

private val MES = DateTimeFormatter.ofPattern("yyyy-MM")

/**
 * Área del modelo, escala de notas, interés y método de una asignatura (una sola hoja, nada obligatorio).
 * El interés se guarda una vez por mes: responder de nuevo ese mes corrige la respuesta, el mes siguiente
 * añade otra al historial. El método se guarda una vez por día.
 */
class GuardarDatosAsignaturaUseCase @Inject constructor(
    private val asignaturas: AsignaturasRepository,
    private val percepciones: PercepcionesRepository,
    private val gamificacion: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    suspend operator fun invoke(
        asignaturaId: Int, area: String?, escalaMin: Float?, escalaMax: Float?, interes: Float?, metodo: String?
    ): ResultadoGuardarDatos {
        sesion.uid() ?: return ResultadoGuardarDatos.SinSesion
        if (area != null && !CatalogoModelo.esArea(area)) return ResultadoGuardarDatos.Invalido("Elige un área de la lista")
        if ((escalaMin == null) != (escalaMax == null)) return ResultadoGuardarDatos.Invalido("Indica el mínimo y el máximo de la escala")
        if (escalaMin != null && escalaMax != null && escalaMax <= escalaMin) {
            return ResultadoGuardarDatos.Invalido("El máximo de la escala debe ser mayor que el mínimo")
        }
        if (interes != null && interes !in 0f..10f) return ResultadoGuardarDatos.Invalido("Interés fuera de rango")
        if (metodo != null && !CatalogoModelo.esMetodo(metodo)) return ResultadoGuardarDatos.Invalido("Elige un método de la lista")

        return try {
            gamificacion.enTransaccion {
                val asignatura = asignaturas.getAsignatura(asignaturaId)
                    ?: return@enTransaccion ResultadoGuardarDatos.Invalido("La asignatura ya no existe")
                asignaturas.guardarDatosModelo(asignaturaId, area ?: asignatura.areaModelo, escalaMin, escalaMax)
                val ahora = reloj.ahora()
                val millis = ahora.toInstant().toEpochMilli()
                if (interes != null) {
                    percepciones.guardar(
                        ReglasExtraccion.TIPO_INTERES, "INTERES:$asignaturaId:${ahora.format(MES)}", asignaturaId, null, interes, null, millis
                    )
                }
                if (metodo != null) {
                    percepciones.guardar(
                        ReglasExtraccion.TIPO_METODO, "METODO:$asignaturaId:${ahora.toLocalDate()}", asignaturaId, null, null, metodo, millis
                    )
                }
                ResultadoGuardarDatos.Guardado
            }
        } catch (e: Exception) {
            ResultadoGuardarDatos.Error
        }
    }
}

/**
 * Dificultad percibida. Al entregar o calificar una tarea la pregunta es de esa tarea (TAREA:<id>);
 * desde el panel de datos, del tema o la asignatura en el día (DECLARADA:...). Repetir corrige la respuesta.
 */
class GuardarDificultadUseCase @Inject constructor(
    private val asignaturas: AsignaturasRepository,
    private val percepciones: PercepcionesRepository,
    private val reloj: Reloj
) {
    suspend fun deTarea(tarea: Tarea, valor: Float): ResultadoGuardarDatos {
        val tareaId = tarea.id ?: return ResultadoGuardarDatos.Invalido("Tarea sin guardar")
        val asignaturaId = tarea.asignaturaId ?: asignaturas.idPorNombre(tarea.asignatura)
            ?: return ResultadoGuardarDatos.Invalido("La tarea no pertenece a una asignatura registrada")
        return guardar(asignaturaId, tarea.tema, valor, "TAREA:$tareaId")
    }

    suspend fun deAsignatura(asignaturaId: Int, tema: String?, valor: Float): ResultadoGuardarDatos {
        val ahora = reloj.ahora()
        return guardar(asignaturaId, tema, valor, "DECLARADA:$asignaturaId:${tema ?: "-"}:${ahora.toLocalDate()}")
    }

    /** Para no volver a preguntar por una tarea ya respondida. */
    suspend fun yaRespondida(tareaId: Int): Boolean = percepciones.existe(ReglasExtraccion.TIPO_DIFICULTAD, "TAREA:$tareaId")

    private suspend fun guardar(asignaturaId: Int, tema: String?, valor: Float, origen: String): ResultadoGuardarDatos {
        if (valor !in 0f..10f) return ResultadoGuardarDatos.Invalido("Dificultad fuera de rango")
        return try {
            val millis = reloj.ahora().toInstant().toEpochMilli()
            if (percepciones.guardar(ReglasExtraccion.TIPO_DIFICULTAD, origen, asignaturaId, tema, valor, null, millis)) {
                ResultadoGuardarDatos.Guardado
            } else ResultadoGuardarDatos.SinSesion
        } catch (e: Exception) {
            ResultadoGuardarDatos.Error
        }
    }
}

/**
 * Estudio hecho fuera del temporizador (biblioteca, grupo de estudio...). Cuenta como tiempo de estudio
 * para las predicciones, pero no da EduCoins ni cuenta para misiones o logros: solo se puede registrar
 * lo de los últimos [DIAS_ATRAS] días y con una duración razonable.
 */
class RegistrarEstudioManualUseCase @Inject constructor(
    private val asignaturas: AsignaturasRepository,
    private val gamificacion: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    companion object {
        const val DIAS_ATRAS = 7L
        const val MIN_MINUTOS = 5
        const val MAX_MINUTOS = 300
    }

    suspend operator fun invoke(asignaturaId: Int, fecha: LocalDate, minutos: Int, metodo: String?): ResultadoGuardarDatos {
        val uid = sesion.uid() ?: return ResultadoGuardarDatos.SinSesion
        val ahora = reloj.ahora()
        val hoy = ahora.toLocalDate()
        if (fecha.isAfter(hoy) || fecha.isBefore(hoy.minusDays(DIAS_ATRAS))) {
            return ResultadoGuardarDatos.Invalido("Solo puedes registrar estudio de los últimos $DIAS_ATRAS días")
        }
        if (minutos !in MIN_MINUTOS..MAX_MINUTOS) {
            return ResultadoGuardarDatos.Invalido("La duración debe estar entre $MIN_MINUTOS y $MAX_MINUTOS minutos")
        }
        if (metodo != null && !CatalogoModelo.esMetodo(metodo)) return ResultadoGuardarDatos.Invalido("Elige un método de la lista")

        return try {
            gamificacion.enTransaccion {
                val asignatura = asignaturas.getAsignatura(asignaturaId)
                    ?: return@enTransaccion ResultadoGuardarDatos.Invalido("La asignatura ya no existe")
                // Termina como tarde ahora: no se registra estudio que aún no ha pasado
                val fin = minOf(fecha.atTime(ahora.toLocalTime()).atZone(ahora.zone).toInstant().toEpochMilli(), ahora.toInstant().toEpochMilli())
                var inicio = fin - minutos * 60_000L
                // La clave es (uid, inicio): se desplaza un milisegundo si ya hay una sesión que empieza igual
                while (gamificacion.existeSesion(uid, inicio)) inicio++
                gamificacion.insertarSesionEnfoque(
                    SesionEnfoqueEntity(
                        uid, inicio, minutos, asignatura.nombre, inicio + minutos * 60_000L,
                        asignaturaId = asignaturaId, metodo = metodo, origen = SesionEnfoqueEntity.ORIGEN_MANUAL
                    )
                )
                ResultadoGuardarDatos.Guardado
            }
        } catch (e: Exception) {
            ResultadoGuardarDatos.Error
        }
    }
}
