package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.data.database.entities.AsistenciaEntity
import com.softcg.myapplication.data.database.entities.PercepcionEntity
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Rango numérico de una escala de notas, p. ej. 1-5 o 0-100. */
data class Escala(val min: Float, val max: Float) {
    fun contiene(valor: Float) = valor in min..max

    /** Lleva el valor a la escala 0-10 del modelo. */
    fun a10(valor: Float): Double = ((valor - min) / (max - min) * 10f).toDouble()
}

data class MedidaAsistencia(val respondidas: Int, val asistidas: Int, val recientesRespondidas: Int, val recientesAsistidas: Int) {
    val porcentaje: Double get() = if (respondidas == 0) 0.0 else asistidas * 100.0 / respondidas
    val porcentajeReciente: Double? get() = if (recientesRespondidas == 0) null else recientesAsistidas * 100.0 / recientesRespondidas
}

data class MedidaNotas(val promedio10: Double?, val usadas: Int, val sinEscala: Int, val fueraDeEscala: Int)

data class MedidaIntensidad(val horasPorSemana: Double, val diasDeHistorial: Long, val sesiones: Int)

enum class OrigenHoras { SESIONES, DECLARADAS }

/**
 * Cálculo de los parámetros del modelo a partir de los registros guardados. Funciones puras: reciben
 * los registros de una asignatura y la fecha actual, así que se prueban sin base de datos.
 *
 * SUPUESTOS a confirmar con quien entrenó el modelo (el dataset es sintético y no define unidades):
 * - attendance_avg = porcentaje de clases asistidas / 10 (escala 0-10 como las demás variables).
 * - intensity_hours = horas de estudio por semana de la asignatura (rango de entrenamiento 1-6).
 */
object ReglasExtraccion {

    // Datos mínimos antes de que un parámetro se considere suficiente
    const val MIN_CLASES = 4
    const val MIN_NOTAS = 2
    const val MIN_DIAS_HISTORIAL_ESTUDIO = 14L
    const val VENTANA_ESTUDIO_DIAS = 28L
    const val VENTANA_ASISTENCIA_RECIENTE_DIAS = 28L
    const val VIGENCIA_INTERES_DIAS = 60L
    const val RESPUESTAS_DIFICULTAD = 5
    const val EVENTOS_METODO = 5

    const val TIPO_INTERES = "INTERES"
    const val TIPO_DIFICULTAD = "DIFICULTAD"
    const val TIPO_METODO = "METODO"
    const val TIPO_HORAS = "HORAS_ESTUDIO"

    /** Mientras no haya [MIN_CLASES] clases respondidas se supone que asiste a todas. */
    const val ASISTENCIA_SUPUESTA = 100.0

    /** Horas por semana que el estudiante puede declarar. */
    const val MAX_HORAS_SEMANA = 40f

    // ASISTENCIA
    fun asistencia(registros: List<AsistenciaEntity>, hoy: LocalDate): MedidaAsistencia {
        val desde = hoy.minusDays(VENTANA_ASISTENCIA_RECIENTE_DIAS)
        val recientes = registros.filter { runCatching { LocalDate.parse(it.fecha) }.getOrNull()?.isAfter(desde) == true }
        return MedidaAsistencia(registros.size, registros.count { it.asistio }, recientes.size, recientes.count { it.asistio })
    }

    /** Con suficientes clases, el porcentaje real; antes, [ASISTENCIA_SUPUESTA]. Se envía en escala 0-10. */
    fun asistenciaModelo(medida: MedidaAsistencia): Double =
        (if (asistenciaMedida(medida)) medida.porcentaje else ASISTENCIA_SUPUESTA) / 10.0

    fun asistenciaMedida(medida: MedidaAsistencia): Boolean = medida.respondidas >= MIN_CLASES

    // TIEMPO DE ESTUDIO PARA EL MODELO
    /**
     * Las sesiones registradas mandan cuando ya hay [MIN_DIAS_HISTORIAL_ESTUDIO] días de historial;
     * antes, las horas por semana que el estudiante declaró. Null si no hay ninguna de las dos.
     */
    fun horasParaModelo(medida: MedidaIntensidad?, declaradas: PercepcionEntity?): Pair<Double, OrigenHoras>? = when {
        medida != null && medida.diasDeHistorial >= MIN_DIAS_HISTORIAL_ESTUDIO -> medida.horasPorSemana to OrigenHoras.SESIONES
        declaradas?.valor != null -> declaradas.valor.toDouble() to OrigenHoras.DECLARADAS
        else -> null
    }

    /** Las últimas horas por semana declaradas. */
    fun horasDeclaradas(percepciones: List<PercepcionEntity>): PercepcionEntity? =
        percepciones.filter { it.tipo == TIPO_HORAS && it.valor != null }.maxByOrNull { it.fechaMillis }

    // NOTAS
    private val RANGO = Regex("""(\d+(?:[.,]\d+)?)\s*(?:[-–]|a)\s*(\d+(?:[.,]\d+)?)""")

    /** "Numérico (0-10)" → 0..10, "Porcentaje (0-100%)" → 0..100, "1 a 5" → 1..5; null si no indica un rango. */
    fun escalaDeTexto(texto: String?): Escala? {
        val coincidencia = RANGO.find(texto ?: return null) ?: return null
        val min = coincidencia.groupValues[1].replace(',', '.').toFloatOrNull() ?: return null
        val max = coincidencia.groupValues[2].replace(',', '.').toFloatOrNull() ?: return null
        return if (max > min) Escala(min, max) else null
    }

    /**
     * Escala de una nota: la de su propio tipo si la indica ("Numerico (1-5)"), si no la que el estudiante
     * fijó para la asignatura y, por último, la del tipo de calificación de la asignatura.
     */
    fun escalaDe(tipoNota: String, escalaMin: Float?, escalaMax: Float?, tipoAsignatura: String): Escala? {
        escalaDeTexto(tipoNota)?.let { return it }
        if (escalaMin != null && escalaMax != null && escalaMax > escalaMin) return Escala(escalaMin, escalaMax)
        return escalaDeTexto(tipoAsignatura)
    }

    /** Promedio en 0-10. Las notas sin escala conocida o fuera de su escala no se usan (no se adivinan). */
    fun notas(valores: List<Pair<Float, Escala?>>): MedidaNotas {
        val sinEscala = valores.count { it.second == null }
        val conEscala = valores.filter { it.second != null }
        val validas = conEscala.filter { (valor, escala) -> escala!!.contiene(valor) }
        val promedio = if (validas.isEmpty()) null else validas.map { (valor, escala) -> escala!!.a10(valor) }.average()
        return MedidaNotas(promedio, validas.size, sinEscala, conEscala.size - validas.size)
    }

    // TIEMPO DE ESTUDIO
    /**
     * Horas por semana en las últimas [VENTANA_ESTUDIO_DIAS] (o desde la primera sesión, si es más reciente).
     * Una semana sin estudiar cuenta como 0: es un dato real, no uno faltante.
     */
    fun intensidad(sesiones: List<SesionEnfoqueEntity>, ahoraMillis: Long, zona: ZoneId): MedidaIntensidad? {
        if (sesiones.isEmpty()) return null
        val hoy = Instant.ofEpochMilli(ahoraMillis).atZone(zona).toLocalDate()
        val primera = Instant.ofEpochMilli(sesiones.minOf { it.inicioMillis }).atZone(zona).toLocalDate()
        val historial = ChronoUnit.DAYS.between(primera, hoy) + 1
        val ventanaDias = minOf(VENTANA_ESTUDIO_DIAS, historial)
        val desde = hoy.minusDays(ventanaDias - 1).atStartOfDay(zona).toInstant().toEpochMilli()
        val enVentana = sesiones.filter { it.inicioMillis >= desde }
        val horas = enVentana.sumOf { it.minutos } / 60.0
        return MedidaIntensidad(horas / (ventanaDias / 7.0), historial, enVentana.size)
    }

    // PERCEPCIONES
    /** El interés más reciente, si no tiene más de [VIGENCIA_INTERES_DIAS]. */
    fun interes(percepciones: List<PercepcionEntity>): PercepcionEntity? =
        percepciones.filter { it.tipo == TIPO_INTERES && it.valor != null }.maxByOrNull { it.fechaMillis }

    fun interesVigente(interes: PercepcionEntity, ahoraMillis: Long): Boolean =
        ahoraMillis - interes.fechaMillis <= VIGENCIA_INTERES_DIAS * 24 * 60 * 60_000L

    /** Media de las últimas respuestas del tema; si el tema aún no tiene, las de la asignatura. */
    fun dificultad(percepciones: List<PercepcionEntity>, tema: String?): Pair<Double, Int>? {
        val todas = percepciones.filter { it.tipo == TIPO_DIFICULTAD && it.valor != null }.sortedByDescending { it.fechaMillis }
        val delTema = if (tema != null) todas.filter { it.tema == tema } else emptyList()
        val usadas = (delTema.ifEmpty { todas }).take(RESPUESTAS_DIFICULTAD)
        if (usadas.isEmpty()) return null
        return usadas.map { it.valor!!.toDouble() }.average() to usadas.size
    }

    /**
     * El método más usado en las últimas [EVENTOS_METODO] sesiones o declaraciones; en empate, el más reciente.
     * Solo métodos del catálogo: lo que el estudiante no indicó no se rellena.
     */
    fun metodo(sesiones: List<SesionEnfoqueEntity>, percepciones: List<PercepcionEntity>): String? {
        val eventos = sesiones.filter { CatalogoModelo.esMetodo(it.metodo) }.map { it.metodo!! to it.inicioMillis } +
            percepciones.filter { it.tipo == TIPO_METODO && CatalogoModelo.esMetodo(it.texto) }.map { it.texto!! to it.fechaMillis }
        val ultimos = eventos.sortedByDescending { it.second }.take(EVENTOS_METODO)
        if (ultimos.isEmpty()) return null
        val conteo = ultimos.groupingBy { it.first }.eachCount()
        val maximo = conteo.values.max()
        return ultimos.first { conteo[it.first] == maximo }.first
    }
}
