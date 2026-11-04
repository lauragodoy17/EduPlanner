package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.database.entities.AsistenciaEntity
import com.softcg.myapplication.data.network.SesionUsuario
import java.time.LocalDate
import javax.inject.Inject

class ObtenerClasesPorConfirmarUseCase @Inject constructor(
    private val asignaturasRepository: AsignaturasRepository,
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    /** Se calcula siempre con los datos guardados y la hora actual, no con lo que haya mostrado la pantalla. */
    suspend operator fun invoke(): List<ClaseProgramada> {
        val uid = sesion.uid() ?: return emptyList()
        val ahora = reloj.ahora()
        val respondidas = repository.asistencias(uid).map { it.asignaturaId to LocalDate.parse(it.fecha) }.toSet()
        return AsistenciaReglas.clasesPorConfirmar(
            asignaturasRepository.getAllAsignaturas(), respondidas, ahora.toLocalDateTime(), ahora.zone
        )
    }
}

sealed class ResultadoAsistencia {
    data class Registrada(val asistio: Boolean, val monedas: Int) : ResultadoAsistencia()
    object YaRespondida : ResultadoAsistencia()
    object AunNoTermina : ResultadoAsistencia()
    object PlazoVencido : ResultadoAsistencia()
    object ClaseNoExiste : ResultadoAsistencia()
    object SinSesion : ResultadoAsistencia()
    object Error : ResultadoAsistencia()
}

class ConfirmarAsistenciaUseCase @Inject constructor(
    private val asignaturasRepository: AsignaturasRepository,
    private val repository: GamificacionRepository,
    private val otorgarRecompensa: OtorgarRecompensaUseCase,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    /**
     * La clase se vuelve a calcular a partir de la asignatura guardada y la hora actual:
     * no basta con que el modal esté abierto. La respuesta es definitiva y la recompensa
     * por «Sí» se guarda en la misma transacción que la asistencia.
     */
    suspend operator fun invoke(asignaturaId: Int, fecha: LocalDate, asistio: Boolean): ResultadoAsistencia {
        val uid = sesion.uid() ?: return ResultadoAsistencia.SinSesion
        return try {
            val asignatura = asignaturasRepository.getAsignatura(asignaturaId)
                ?: return ResultadoAsistencia.ClaseNoExiste
            val ahora = reloj.ahora()
            val clase = AsistenciaReglas.claseEnFecha(asignatura, fecha, ahora.zone)
                ?: return ResultadoAsistencia.ClaseNoExiste

            when (AsistenciaReglas.estado(clase, null, ahora.toLocalDateTime())) {
                EstadoAsistencia.PENDIENTE -> return ResultadoAsistencia.AunNoTermina
                EstadoAsistencia.VENCIDA -> return ResultadoAsistencia.PlazoVencido
                else -> {}
            }

            repository.enTransaccion {
                if (repository.asistencia(uid, asignaturaId, fecha.toString()) != null) {
                    return@enTransaccion ResultadoAsistencia.YaRespondida
                }
                repository.insertarAsistencia(
                    AsistenciaEntity(
                        uid = uid,
                        asignaturaId = asignaturaId,
                        fecha = fecha.toString(),
                        asistio = asistio,
                        inicioMillis = clase.inicio.atZone(ahora.zone).toInstant().toEpochMilli(),
                        finMillis = clase.fin.atZone(ahora.zone).toInstant().toEpochMilli(),
                        confirmadaMillis = ahora.toInstant().toEpochMilli()
                    )
                )
                val monedas = if (asistio) {
                    val recompensa = otorgarRecompensa.otorgarEnTransaccion(uid, TipoRecompensa.ASISTENCIA, "$asignaturaId:$fecha")
                    (recompensa as? ResultadoRecompensa.Otorgada)?.cantidad ?: 0
                } else 0
                ResultadoAsistencia.Registrada(asistio, monedas)
            }
        } catch (e: Exception) {
            ResultadoAsistencia.Error
        }
    }
}
