package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.SesionUsuario
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class MisionEstado(val mision: MisionDef, val avance: Int, val reclamada: Boolean) {
    val completada: Boolean get() = avance >= mision.meta
    val reclamable: Boolean get() = completada && !reclamada
}

data class PanelMisiones(
    val diarias: List<MisionEstado>,
    val semanales: List<MisionEstado>,
    val bonusReclamado: Boolean
) {
    val porReclamar: Int get() = (diarias + semanales).count { it.reclamable }
    val diariasReclamadas: Int get() = diarias.count { it.reclamada }
}

class ObtenerMisionesUseCase @Inject constructor(
    private val asignaturasRepository: AsignaturasRepository,
    private val tareasRepository: TareasRepository,
    private val repository: GamificacionRepository,
    private val reloj: Reloj
) {
    /** La primera consulta de cada periodo elige las misiones y las guarda; después ya no cambian. */
    suspend operator fun invoke(uid: String): PanelMisiones {
        val ahora = reloj.ahora()
        val hoy = ahora.toLocalDate()
        val diarias = estados(uid, PeriodoMision.DIARIA, hoy, ahora.zone)
        val semanales = estados(uid, PeriodoMision.SEMANAL, hoy, ahora.zone)
        return PanelMisiones(diarias, semanales, repository.existeMovimiento(uid, claveBonus(hoy)))
    }

    private suspend fun estados(uid: String, periodo: PeriodoMision, hoy: LocalDate, zona: ZoneId): List<MisionEstado> {
        val misiones = asignadas(uid, periodo, hoy, zona)
        val desde = periodo.inicio(hoy).toString()
        val conteos = repository.conteoPorTipoDesde(uid, desde)
        val diasActivos = repository.diasConTipos(uid, TIPOS_ACTIVIDAD).count { it >= desde }
        return misiones.map { mision ->
            val avance = if (mision.metrica == MetricaMision.DIAS_ACTIVOS) diasActivos
            else mision.metrica.tipos.sumOf { conteos[it] ?: 0 }
            MisionEstado(mision, avance.coerceAtMost(mision.meta), repository.existeMovimiento(uid, mision.clave(hoy)))
        }
    }

    private suspend fun asignadas(uid: String, periodo: PeriodoMision, hoy: LocalDate, zona: ZoneId): List<MisionDef> {
        val clave = periodo.clave(hoy)
        return repository.enTransaccion {
            val guardadas = repository.misionesAsignadas(uid, clave)
            if (guardadas.isNotEmpty()) {
                guardadas.mapNotNull { MisionesConfig.buscar(it) }.sortedBy { MisionesConfig.misiones.indexOf(it) }
            } else {
                val elegidas = MisionesConfig.seleccionar(periodo, contexto(hoy, zona), (uid + clave).hashCode())
                repository.asignarMisiones(uid, clave, elegidas.map { it.id })
                elegidas.sortedBy { MisionesConfig.misiones.indexOf(it) }
            }
        }
    }

    private fun contexto(hoy: LocalDate, zona: ZoneId): ContextoMisiones {
        val asignaturas = asignaturasRepository.getAllAsignaturas()
        val pendientes = tareasRepository.getTareas().filter { !it.completada }
        val finSemana = RachaReglas.lunes(hoy).plusDays(6)
        val diasRestantes = generateSequence(hoy) { it.plusDays(1) }.takeWhile { !it.isAfter(finSemana) }.toList()
        return ContextoMisiones(
            clasesHoy = asignaturas.count { AsistenciaReglas.claseEnFecha(it, hoy, zona) != null },
            clasesRestantesSemana = diasRestantes.sumOf { dia -> asignaturas.count { AsistenciaReglas.claseEnFecha(it, dia, zona) != null } },
            tareasPendientes = pendientes.size,
            tareasPorVencer = pendientes.count { ReglasEntrega.aTiempo(it.fecha, hoy) }
        )
    }

    companion object {
        fun claveBonus(hoy: LocalDate) = "$TIPO_MISION_BONUS:$hoy"
    }
}

sealed class ResultadoMision {
    data class Reclamada(val monedas: Int, val bonus: Int) : ResultadoMision()
    object Incompleta : ResultadoMision()
    object YaReclamada : ResultadoMision()
    object NoDisponible : ResultadoMision()
    object SinSesion : ResultadoMision()
    object Error : ResultadoMision()
}

class ReclamarMisionUseCase @Inject constructor(
    private val obtenerMisiones: ObtenerMisionesUseCase,
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    /** El avance se recalcula con lo guardado: no basta con que la pantalla muestre el botón. */
    suspend operator fun invoke(misionId: String): ResultadoMision {
        val uid = sesion.uid() ?: return ResultadoMision.SinSesion
        return try {
            repository.enTransaccion {
                val ahora = reloj.ahora()
                val hoy = ahora.toLocalDate()
                val millis = ahora.toInstant().toEpochMilli()
                val panel = obtenerMisiones(uid)
                val estado = (panel.diarias + panel.semanales).firstOrNull { it.mision.id == misionId }
                    ?: return@enTransaccion ResultadoMision.NoDisponible
                if (estado.reclamada) return@enTransaccion ResultadoMision.YaReclamada
                if (!estado.completada) return@enTransaccion ResultadoMision.Incompleta

                val mision = estado.mision
                val insertado = repository.insertarMovimiento(
                    uid, mision.clave(hoy), mision.periodo.tipoMovimiento, mision.recompensa,
                    "Misión: ${mision.titulo}", millis, hoy.toString()
                )
                if (!insertado) return@enTransaccion ResultadoMision.YaReclamada

                val bonus = if (mision.periodo == PeriodoMision.DIARIA && panel.diarias.all { it.reclamada || it.mision.id == misionId }) {
                    val pagado = repository.insertarMovimiento(
                        uid, ObtenerMisionesUseCase.claveBonus(hoy), TIPO_MISION_BONUS, MisionesConfig.BONUS_DIARIO,
                        "Completaste todas las misiones del día", millis, hoy.toString()
                    )
                    if (pagado) MisionesConfig.BONUS_DIARIO else 0
                } else 0
                ResultadoMision.Reclamada(mision.recompensa, bonus)
            }
        } catch (e: Exception) {
            ResultadoMision.Error
        }
    }
}
