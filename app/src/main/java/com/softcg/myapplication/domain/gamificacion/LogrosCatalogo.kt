package com.softcg.myapplication.domain.gamificacion

enum class RarezaLogro(val nombre: String, val recompensa: Int) {
    BRONCE("Bronce", 10),
    PLATA("Plata", 25),
    ORO("Oro", 60)
}

enum class MetricaLogro(val tipos: List<String> = emptyList()) {
    TAREAS_REGISTRADAS(listOf(TipoRecompensa.TAREA_REGISTRADA.name)),
    TAREAS_TERMINADAS(listOf(TipoRecompensa.TAREA_COMPLETADA.name)),
    TAREAS_A_TIEMPO(listOf(TipoRecompensa.TAREA_A_TIEMPO.name)),
    EVENTOS(listOf(TipoRecompensa.EVENTO_REGISTRADO.name)),
    CLASES(listOf(TipoRecompensa.CLASE_REGISTRADA.name)),
    CALIFICACIONES(listOf(TipoRecompensa.CALIFICACION_REGISTRADA.name)),
    ASISTENCIAS(listOf(TipoRecompensa.ASISTENCIA.name)),
    SESIONES_ENFOQUE(TIPOS_ENFOQUE),
    MISIONES(listOf(TIPO_MISION_DIARIA, TIPO_MISION_SEMANAL)),
    MEJOR_RACHA,
    NIVEL
}

data class Logro(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val rareza: RarezaLogro,
    val metrica: MetricaLogro,
    val meta: Int
) {
    val clave: String get() = "$TIPO_LOGRO:$id"
}

/** Lo que el usuario ha hecho en total; los logros se comparan contra esto. */
data class ContextoLogros(val conteos: Map<String, Int>, val mejorRacha: Int, val nivel: Int) {
    fun valor(metrica: MetricaLogro): Int = when (metrica) {
        MetricaLogro.MEJOR_RACHA -> mejorRacha
        MetricaLogro.NIVEL -> nivel
        else -> metrica.tipos.sumOf { conteos[it] ?: 0 }
    }
}

/** Fuente de los logros. Se conceden una sola vez (clave "LOGRO:<id>") y nunca se pierden. */
object LogrosCatalogo {

    val logros = listOf(
        Logro("primer_paso", "Primer paso", "Registra tu primera tarea", RarezaLogro.BRONCE, MetricaLogro.TAREAS_REGISTRADAS, 1),
        Logro("planificador", "Planificador", "Registra 25 tareas", RarezaLogro.PLATA, MetricaLogro.TAREAS_REGISTRADAS, 25),
        Logro("semestre_armado", "Semestre armado", "Registra 4 asignaturas con su horario", RarezaLogro.BRONCE, MetricaLogro.CLASES, 4),
        Logro("agenda_clara", "Agenda clara", "Registra 10 eventos", RarezaLogro.BRONCE, MetricaLogro.EVENTOS, 10),

        Logro("primera_entrega", "Primera entrega", "Termina tu primera tarea", RarezaLogro.BRONCE, MetricaLogro.TAREAS_TERMINADAS, 1),
        Logro("al_dia", "Al día", "Termina 15 tareas", RarezaLogro.PLATA, MetricaLogro.TAREAS_TERMINADAS, 15),
        Logro("imparable", "Imparable", "Termina 60 tareas", RarezaLogro.ORO, MetricaLogro.TAREAS_TERMINADAS, 60),
        Logro("con_tiempo", "Con tiempo", "Entrega 5 tareas antes de que venzan", RarezaLogro.BRONCE, MetricaLogro.TAREAS_A_TIEMPO, 5),
        Logro("sin_procrastinar", "Sin procrastinar", "Entrega 30 tareas antes de que venzan", RarezaLogro.ORO, MetricaLogro.TAREAS_A_TIEMPO, 30),

        Logro("presente", "Presente", "Asiste a 5 clases", RarezaLogro.BRONCE, MetricaLogro.ASISTENCIAS, 5),
        Logro("buen_habito", "Buen hábito", "Asiste a 25 clases", RarezaLogro.PLATA, MetricaLogro.ASISTENCIAS, 25),
        Logro("siempre_ahi", "Siempre ahí", "Asiste a 80 clases", RarezaLogro.ORO, MetricaLogro.ASISTENCIAS, 80),

        Logro("primer_enfoque", "Primer enfoque", "Completa una sesión de enfoque", RarezaLogro.BRONCE, MetricaLogro.SESIONES_ENFOQUE, 1),
        Logro("trabajo_profundo", "Trabajo profundo", "Completa 15 sesiones de enfoque", RarezaLogro.PLATA, MetricaLogro.SESIONES_ENFOQUE, 15),
        Logro("maraton", "Maratón", "Completa 50 sesiones de enfoque", RarezaLogro.ORO, MetricaLogro.SESIONES_ENFOQUE, 50),

        Logro("racha_3", "Arrancando", "Mantén una racha de 3 días", RarezaLogro.BRONCE, MetricaLogro.MEJOR_RACHA, 3),
        Logro("racha_7", "Una semana firme", "Mantén una racha de 7 días", RarezaLogro.PLATA, MetricaLogro.MEJOR_RACHA, 7),
        Logro("racha_21", "Es un hábito", "Mantén una racha de 21 días", RarezaLogro.ORO, MetricaLogro.MEJOR_RACHA, 21),

        Logro("registro_notas", "Sin perder la cuenta", "Registra 5 calificaciones", RarezaLogro.BRONCE, MetricaLogro.CALIFICACIONES, 5),
        Logro("misiones_10", "Cumplidor", "Reclama 10 misiones", RarezaLogro.BRONCE, MetricaLogro.MISIONES, 10),
        Logro("misiones_60", "Estratega del semestre", "Reclama 60 misiones", RarezaLogro.ORO, MetricaLogro.MISIONES, 60),

        Logro("nivel_5", "A mitad de camino", "Llega al nivel 5", RarezaLogro.PLATA, MetricaLogro.NIVEL, 5),
        Logro("nivel_10", "Summa Cum Laude", "Llega al nivel 10", RarezaLogro.ORO, MetricaLogro.NIVEL, 10)
    )

    fun buscar(id: String): Logro? = logros.firstOrNull { it.id == id }
}
