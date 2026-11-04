package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import javax.inject.Inject

sealed class ResultadoCalificarTarea {
    data class Completada(val monedas: Int, val aTiempo: Boolean = false) : ResultadoCalificarTarea()
    object YaCalificada : ResultadoCalificarTarea()
    object ValorInvalido : ResultadoCalificarTarea()
    object Error : ResultadoCalificarTarea()
}

/**
 * Registrar la calificación de una tarea la da por completada. La calificación aparece también en
 * Calificaciones (es el mismo registro). Si la tarea no se había entregado da la recompensa de tarea
 * completada; si ya se había entregado, esa se cobró al entregar y se da la de calificación registrada.
 */
class CalificarTareaUseCase @Inject constructor(
    private val calificacionesRepository: CalificacionesRespository,
    private val gamificacionRepository: GamificacionRepository,
    private val otorgarRecompensa: OtorgarRecompensaUseCase,
    private val sesion: SesionUsuario
) {
    companion object {
        const val TIPO_CALIFICACION_TAREA = "Tarea"

        /** Acepta coma o punto decimal; no admite valores negativos. */
        fun valorDesdeTexto(texto: String): Float? =
            texto.trim().replace(',', '.').toFloatOrNull()?.takeIf { it >= 0f }
    }

    suspend operator fun invoke(tarea: Tarea, textoValor: String): ResultadoCalificarTarea {
        val tareaId = tarea.id ?: return ResultadoCalificarTarea.Error
        val valor = valorDesdeTexto(textoValor) ?: return ResultadoCalificarTarea.ValorInvalido
        return try {
            gamificacionRepository.enTransaccion {
                if (calificacionesRepository.tareaTieneCalificacion(tareaId)) {
                    return@enTransaccion ResultadoCalificarTarea.YaCalificada
                }
                calificacionesRepository.insertCalificacion(
                    Calificacion(null, TIPO_CALIFICACION_TAREA, valor, tarea.asignatura, tarea.titulo, tareaId)
                )
                val uid = sesion.uid() ?: return@enTransaccion ResultadoCalificarTarea.Completada(0)
                if (tarea.entregadaMillis != null) {
                    val recompensa = otorgarRecompensa.otorgarEnTransaccion(uid, TipoRecompensa.CALIFICACION_REGISTRADA, "tarea-$tareaId")
                    ResultadoCalificarTarea.Completada((recompensa as? ResultadoRecompensa.Otorgada)?.cantidad ?: 0)
                } else {
                    val terminada = otorgarRecompensa.otorgarTareaTerminadaEnTransaccion(uid, tareaId, tarea.fecha)
                    ResultadoCalificarTarea.Completada(terminada.monedas, terminada.aTiempo)
                }
            }
        } catch (e: Exception) {
            ResultadoCalificarTarea.Error
        }
    }
}

sealed class ResultadoEntregarTarea {
    data class Entregada(val monedas: Int, val aTiempo: Boolean) : ResultadoEntregarTarea()
    object YaEntregada : ResultadoEntregarTarea()
    object Error : ResultadoEntregarTarea()
}

/**
 * Marca una tarea como entregada sin esperar la nota, que suele llegar semanas después:
 * así el bono a tiempo premia el momento real de la entrega.
 */
class EntregarTareaUseCase @Inject constructor(
    private val tareasRepository: TareasRepository,
    private val gamificacionRepository: GamificacionRepository,
    private val otorgarRecompensa: OtorgarRecompensaUseCase,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    suspend operator fun invoke(tarea: Tarea): ResultadoEntregarTarea {
        val tareaId = tarea.id ?: return ResultadoEntregarTarea.Error
        return try {
            gamificacionRepository.enTransaccion {
                val ahora = reloj.ahora()
                if (!tareasRepository.marcarEntregada(tareaId, ahora.toInstant().toEpochMilli())) {
                    return@enTransaccion ResultadoEntregarTarea.YaEntregada
                }
                val uid = sesion.uid()
                    ?: return@enTransaccion ResultadoEntregarTarea.Entregada(0, ReglasEntrega.aTiempo(tarea.fecha, ahora.toLocalDate()))
                val terminada = otorgarRecompensa.otorgarTareaTerminadaEnTransaccion(uid, tareaId, tarea.fecha)
                ResultadoEntregarTarea.Entregada(terminada.monedas, terminada.aTiempo)
            }
        } catch (e: Exception) {
            ResultadoEntregarTarea.Error
        }
    }
}
