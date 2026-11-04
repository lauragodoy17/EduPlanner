package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.network.SesionUsuario
import javax.inject.Inject

class OtorgarRecompensaUseCase @Inject constructor(
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {

    /** [referencia] identifica la actividad (p. ej. el id de la tarea); con el tipo forma la clave única. */
    suspend operator fun invoke(tipo: TipoRecompensa, referencia: String): ResultadoRecompensa {
        val uid = sesion.uid() ?: return ResultadoRecompensa.SinSesion
        return try {
            repository.enTransaccion { otorgarEnTransaccion(uid, tipo, referencia) }
        } catch (e: Exception) {
            ResultadoRecompensa.Error
        }
    }

    /** Para usar dentro de otra transacción (p. ej. al confirmar asistencia). */
    suspend fun otorgarEnTransaccion(uid: String, tipo: TipoRecompensa, referencia: String): ResultadoRecompensa {
        val clave = tipo.clave(referencia)
        if (repository.existeMovimiento(uid, clave)) return ResultadoRecompensa.YaOtorgada

        val ahora = reloj.ahora()
        val dia = ahora.toLocalDate().toString()
        if (repository.contarMovimientosDelDia(uid, tipo.name, dia) >= tipo.topeDiario) {
            return ResultadoRecompensa.TopeDiarioAlcanzado
        }

        val insertado = repository.insertarMovimiento(
            uid, clave, tipo.name, tipo.cantidad, tipo.descripcion, ahora.toInstant().toEpochMilli(), dia
        )
        return if (insertado) ResultadoRecompensa.Otorgada(tipo.cantidad) else ResultadoRecompensa.YaOtorgada
    }

    /**
     * Premia terminar una tarea, sea entregándola o calificándola. El bono a tiempo solo se concede
     * junto con la recompensa de completar: borrar la nota y volver a calificar no lo repite.
     */
    suspend fun otorgarTareaTerminadaEnTransaccion(uid: String, tareaId: Int, fechaEntrega: String): TareaTerminada {
        val completada = otorgarEnTransaccion(uid, TipoRecompensa.TAREA_COMPLETADA, "$tareaId")
        if (completada !is ResultadoRecompensa.Otorgada) return TareaTerminada(0, false)

        val aTiempo = ReglasEntrega.aTiempo(fechaEntrega, reloj.ahora().toLocalDate())
        val bono = if (aTiempo) otorgarEnTransaccion(uid, TipoRecompensa.TAREA_A_TIEMPO, "$tareaId") else null
        return TareaTerminada(completada.cantidad + ((bono as? ResultadoRecompensa.Otorgada)?.cantidad ?: 0), aTiempo)
    }
}

data class TareaTerminada(val monedas: Int, val aTiempo: Boolean)
