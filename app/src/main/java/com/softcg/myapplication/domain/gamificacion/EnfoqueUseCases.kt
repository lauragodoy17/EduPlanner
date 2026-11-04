package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.network.SesionUsuario
import javax.inject.Inject

/** Las duraciones disponibles y lo que paga cada una: premiar tiempo real de estudio. */
enum class DuracionEnfoque(val minutos: Int, val tipo: TipoRecompensa) {
    CORTA(15, TipoRecompensa.ENFOQUE_CORTO),
    MEDIA(25, TipoRecompensa.ENFOQUE_MEDIO),
    LARGA(50, TipoRecompensa.ENFOQUE_LARGO);

    companion object {
        fun deMinutos(minutos: Int): DuracionEnfoque? = values().firstOrNull { it.minutos == minutos }
    }
}

data class SesionEnfoque(val inicioMillis: Long, val minutos: Int, val asignatura: String) {
    val finMillis: Long get() = inicioMillis + minutos * 60_000L
}

object EnfoqueReglas {
    /** Una sesión olvidada no se puede cobrar días después: pasado este plazo desde su fin se descarta. */
    const val PLAZO_RECLAMO_MILLIS = 12 * 60 * 60_000L

    fun caducada(sesion: SesionEnfoque, ahoraMillis: Long) = ahoraMillis > sesion.finMillis + PLAZO_RECLAMO_MILLIS
}

sealed class ResultadoIniciarEnfoque {
    data class Iniciada(val sesion: SesionEnfoque) : ResultadoIniciarEnfoque()
    object YaHayUna : ResultadoIniciarEnfoque()
    object DuracionInvalida : ResultadoIniciarEnfoque()
    object SinSesion : ResultadoIniciarEnfoque()
    object Error : ResultadoIniciarEnfoque()
}

sealed class ResultadoCompletarEnfoque {
    data class Completada(val minutos: Int, val monedas: Int) : ResultadoCompletarEnfoque()
    object AunNoTermina : ResultadoCompletarEnfoque()
    object Caducada : ResultadoCompletarEnfoque()
    object SinSesionActiva : ResultadoCompletarEnfoque()
    object Error : ResultadoCompletarEnfoque()
}

class ObtenerEnfoqueActivoUseCase @Inject constructor(
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    /** La sesión en curso sobrevive a cerrar la app porque está guardada, no en la pantalla. */
    suspend operator fun invoke(): SesionEnfoque? {
        val uid = sesion.uid() ?: return null
        val guardada = repository.sesionEnfoqueActiva(uid) ?: return null
        val actual = SesionEnfoque(guardada.inicioMillis, guardada.minutos, guardada.asignatura)
        if (EnfoqueReglas.caducada(actual, reloj.ahora().toInstant().toEpochMilli())) {
            repository.borrarSesionEnfoque(uid, guardada.inicioMillis)
            return null
        }
        return actual
    }
}

class IniciarEnfoqueUseCase @Inject constructor(
    private val obtenerActiva: ObtenerEnfoqueActivoUseCase,
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    suspend operator fun invoke(minutos: Int, asignatura: String): ResultadoIniciarEnfoque {
        val uid = sesion.uid() ?: return ResultadoIniciarEnfoque.SinSesion
        DuracionEnfoque.deMinutos(minutos) ?: return ResultadoIniciarEnfoque.DuracionInvalida
        return try {
            repository.enTransaccion {
                if (obtenerActiva() != null) return@enTransaccion ResultadoIniciarEnfoque.YaHayUna
                val inicio = reloj.ahora().toInstant().toEpochMilli()
                repository.insertarSesionEnfoque(SesionEnfoqueEntity(uid, inicio, minutos, asignatura, null))
                ResultadoIniciarEnfoque.Iniciada(SesionEnfoque(inicio, minutos, asignatura))
            }
        } catch (e: Exception) {
            ResultadoIniciarEnfoque.Error
        }
    }
}

class CompletarEnfoqueUseCase @Inject constructor(
    private val otorgarRecompensa: OtorgarRecompensaUseCase,
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    /** El tiempo se comprueba contra el inicio guardado; la pantalla solo avisa de que ya terminó. */
    suspend operator fun invoke(): ResultadoCompletarEnfoque {
        val uid = sesion.uid() ?: return ResultadoCompletarEnfoque.SinSesionActiva
        return try {
            repository.enTransaccion {
                val guardada = repository.sesionEnfoqueActiva(uid)
                    ?: return@enTransaccion ResultadoCompletarEnfoque.SinSesionActiva
                val actual = SesionEnfoque(guardada.inicioMillis, guardada.minutos, guardada.asignatura)
                val ahora = reloj.ahora().toInstant().toEpochMilli()
                if (ahora < actual.finMillis) return@enTransaccion ResultadoCompletarEnfoque.AunNoTermina
                if (EnfoqueReglas.caducada(actual, ahora)) {
                    repository.borrarSesionEnfoque(uid, guardada.inicioMillis)
                    return@enTransaccion ResultadoCompletarEnfoque.Caducada
                }
                repository.completarSesionEnfoque(uid, guardada.inicioMillis, ahora)
                val tipo = DuracionEnfoque.deMinutos(guardada.minutos)?.tipo
                val recompensa = tipo?.let { otorgarRecompensa.otorgarEnTransaccion(uid, it, "${guardada.inicioMillis}") }
                ResultadoCompletarEnfoque.Completada(guardada.minutos, (recompensa as? ResultadoRecompensa.Otorgada)?.cantidad ?: 0)
            }
        } catch (e: Exception) {
            ResultadoCompletarEnfoque.Error
        }
    }
}

class AbandonarEnfoqueUseCase @Inject constructor(
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario
) {
    /** Abandonar no paga ni penaliza. */
    suspend operator fun invoke(): Boolean {
        val uid = sesion.uid() ?: return false
        return try {
            repository.sesionEnfoqueActiva(uid)?.let { repository.borrarSesionEnfoque(uid, it.inicioMillis) }
            true
        } catch (e: Exception) {
            false
        }
    }
}
