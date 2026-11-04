package com.softcg.myapplication.domain.gamificacion

import java.time.LocalDate
import kotlin.random.Random

enum class PeriodoMision(val tipoMovimiento: String, val cuantas: Int) {
    DIARIA(TIPO_MISION_DIARIA, 3),
    SEMANAL(TIPO_MISION_SEMANAL, 3);

    fun inicio(hoy: LocalDate): LocalDate = if (this == DIARIA) hoy else RachaReglas.lunes(hoy)

    /** Identifica el periodo en curso: "D:2026-11-03" o "S:2026-11-02". */
    fun clave(hoy: LocalDate): String = "${name.first()}:${inicio(hoy)}"
}

/** De dónde sale el avance de una misión: cuántos movimientos de esos tipos hay en el periodo. */
enum class MetricaMision(val tipos: List<String>) {
    TAREAS_REGISTRADAS(listOf(TipoRecompensa.TAREA_REGISTRADA.name)),
    EVENTOS_REGISTRADOS(listOf(TipoRecompensa.EVENTO_REGISTRADO.name)),
    TAREAS_TERMINADAS(listOf(TipoRecompensa.TAREA_COMPLETADA.name)),
    TAREAS_A_TIEMPO(listOf(TipoRecompensa.TAREA_A_TIEMPO.name)),
    ASISTENCIAS(listOf(TipoRecompensa.ASISTENCIA.name)),
    SESIONES_ENFOQUE(TIPOS_ENFOQUE),
    MISIONES_DIARIAS(listOf(TIPO_MISION_DIARIA)),

    /** Se cuenta en días distintos con actividad, no en movimientos. */
    DIAS_ACTIVOS(emptyList())
}

/** Lo que el estudiante puede hacer de verdad en este periodo: no se le pide lo imposible. */
data class ContextoMisiones(
    val clasesHoy: Int = 0,
    val clasesRestantesSemana: Int = 0,
    val tareasPendientes: Int = 0,
    val tareasPorVencer: Int = 0
)

data class MisionDef(
    val id: String,
    val periodo: PeriodoMision,
    val titulo: String,
    val descripcion: String,
    val metrica: MetricaMision,
    val meta: Int,
    val recompensa: Int,
    val aplica: (ContextoMisiones) -> Boolean = { true }
) {
    fun clave(hoy: LocalDate) = "MISION:$id:${periodo.clave(hoy)}"
}

/** Única fuente de las misiones y de sus recompensas. */
object MisionesConfig {

    const val BONUS_DIARIO = 15

    val misiones = listOf(
        MisionDef("d_planifica", PeriodoMision.DIARIA, "Anticípate", "Registra una tarea con su fecha de entrega", MetricaMision.TAREAS_REGISTRADAS, 1, 5),
        MisionDef("d_agenda", PeriodoMision.DIARIA, "Agenda lo que viene", "Registra un evento o examen en tu calendario", MetricaMision.EVENTOS_REGISTRADOS, 1, 5),
        MisionDef("d_enfoque", PeriodoMision.DIARIA, "Modo enfoque", "Completa una sesión de enfoque", MetricaMision.SESIONES_ENFOQUE, 1, 10),
        MisionDef("d_entrega", PeriodoMision.DIARIA, "Cierra un pendiente", "Marca una tarea como entregada desde su menú", MetricaMision.TAREAS_TERMINADAS, 1, 10) { it.tareasPendientes > 0 },
        MisionDef("d_a_tiempo", PeriodoMision.DIARIA, "Sin dejarlo para el final", "Entrega una tarea antes de que venza", MetricaMision.TAREAS_A_TIEMPO, 1, 12) { it.tareasPorVencer > 0 },
        MisionDef("d_clase", PeriodoMision.DIARIA, "Presente", "Ve a clase y confirma tu asistencia", MetricaMision.ASISTENCIAS, 1, 10) { it.clasesHoy > 0 },

        MisionDef("s_entregas", PeriodoMision.SEMANAL, "Semana al día", "Termina 3 tareas esta semana", MetricaMision.TAREAS_TERMINADAS, 3, 30) { it.tareasPendientes >= 3 },
        MisionDef("s_a_tiempo", PeriodoMision.SEMANAL, "Cero afanes", "Entrega 2 tareas antes de que venzan", MetricaMision.TAREAS_A_TIEMPO, 2, 30) { it.tareasPorVencer >= 2 },
        MisionDef("s_clases", PeriodoMision.SEMANAL, "Buena asistencia", "Confirma tu asistencia a 3 clases", MetricaMision.ASISTENCIAS, 3, 35) { it.clasesRestantesSemana >= 3 },
        MisionDef("s_enfoque", PeriodoMision.SEMANAL, "Estudio profundo", "Completa 3 sesiones de enfoque", MetricaMision.SESIONES_ENFOQUE, 3, 30),
        MisionDef("s_constancia", PeriodoMision.SEMANAL, "Constancia", "Ten actividad en 5 días distintos", MetricaMision.DIAS_ACTIVOS, 5, 35),
        MisionDef("s_misiones", PeriodoMision.SEMANAL, "Rutina", "Reclama 5 misiones diarias", MetricaMision.MISIONES_DIARIAS, 5, 30)
    )

    fun buscar(id: String): MisionDef? = misiones.firstOrNull { it.id == id }

    /** Misiones posibles según el contexto, en un orden que depende solo de [semilla]. */
    fun seleccionar(periodo: PeriodoMision, contexto: ContextoMisiones, semilla: Int): List<MisionDef> =
        misiones.filter { it.periodo == periodo && it.aplica(contexto) }
            .shuffled(Random(semilla))
            .take(periodo.cuantas)
}
