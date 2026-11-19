package com.softcg.myapplication.domain.gamificacion

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

/**
 * Única fuente de los valores de recompensa. La clave de cada movimiento es "<TIPO>:<referencia>",
 * única por usuario: la misma actividad nunca se premia dos veces.
 * [topeDiario] limita cuántas recompensas de ese tipo se conceden por día (frena borrar y volver a crear).
 */
enum class TipoRecompensa(val cantidad: Int, val topeDiario: Int, val descripcion: String) {
    TAREA_REGISTRADA(2, 5, "Registraste una tarea"),
    TAREA_COMPLETADA(5, 5, "Completaste una tarea"),
    TAREA_A_TIEMPO(3, 5, "Entregaste una tarea a tiempo"),
    EVENTO_REGISTRADO(2, 3, "Registraste un evento"),
    CLASE_REGISTRADA(5, 2, "Registraste una clase"),
    CALIFICACION_REGISTRADA(3, 5, "Registraste una calificación"),
    ASISTENCIA(10, 6, "Asististe a una clase"),
    ENFOQUE_CORTO(5, 4, "Completaste una sesión de enfoque de 15 min"),
    ENFOQUE_MEDIO(10, 4, "Completaste una sesión de enfoque de 25 min"),
    ENFOQUE_LARGO(20, 2, "Completaste una sesión de enfoque de 50 min");

    fun clave(referencia: String) = "$name:$referencia"
}

/** Solo estos tipos son actividad real del estudiante: las recompensas de misiones o logros no cuentan para la racha. */
val TIPOS_ACTIVIDAD: List<String> = TipoRecompensa.values().map { it.name }

val TIPOS_ENFOQUE: List<String> = listOf(
    TipoRecompensa.ENFOQUE_CORTO.name, TipoRecompensa.ENFOQUE_MEDIO.name, TipoRecompensa.ENFOQUE_LARGO.name
)

/** Una tarea cuenta como a tiempo hasta el final del día de su fecha de entrega. */
object ReglasEntrega {
    /** Sin hora de entrega, la tarea vence al terminar su día. */
    val HORA_LIMITE_POR_DEFECTO: LocalTime = LocalTime.of(23, 59)

    /** Solo por fecha (misiones, listas): a tiempo hasta el final del día de entrega. */
    fun aTiempo(fecha: String, hoy: LocalDate): Boolean =
        runCatching { !hoy.isAfter(LocalDate.parse(fecha)) }.getOrDefault(false)

    /** Momento en que vence: la fecha a la [hora] de entrega o, sin hora válida, a las 23:59. null si la fecha no es válida. */
    fun limite(fecha: String, hora: String?): LocalDateTime? {
        val dia = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return null
        val horaLimite = hora?.let { AsistenciaReglas.parsearHora(it) } ?: HORA_LIMITE_POR_DEFECTO
        return dia.atTime(horaLimite)
    }

    /** Con hora: a tiempo hasta ese minuto incluido. */
    fun aTiempo(fecha: String, hora: String?, ahora: LocalDateTime): Boolean {
        val limite = limite(fecha, hora) ?: return false
        return !ahora.isAfter(limite.withSecond(59).withNano(999_999_999))
    }
}

/** Condiciones para que un registro nuevo reciba su recompensa. */
object CondicionesRecompensa {
    fun tareaValida(titulo: String, fecha: String) = titulo.isNotBlank() && fecha.isNotBlank()

    fun eventoValido(titulo: String, fecha: String) = titulo.isNotBlank() && fecha.isNotBlank()

    fun calificacionValida(valor: Float, asignatura: String) = valor > 0f && asignatura.isNotBlank()

    fun claseValida(hora: String, duracion: Int, horario: List<Boolean>) =
        AsistenciaReglas.parsearHora(hora) != null && duracion > 0 && horario.any { it }
}

/** Tipo de movimiento usado para los cobros de la tienda. */
const val TIPO_COMPRA = "COMPRA"

/** Tipos de movimiento de las recompensas de progreso. */
const val TIPO_MISION_DIARIA = "MISION_DIARIA"
const val TIPO_MISION_SEMANAL = "MISION_SEMANAL"
const val TIPO_MISION_BONUS = "MISION_BONUS"
const val TIPO_LOGRO = "LOGRO"
const val TIPO_NIVEL = "NIVEL"

/**
 * Las recompensas por subir de nivel no suman XP: si sumaran, subir de nivel ayudaría a subir de nivel.
 * Las compras tampoco restan: el XP solo cuenta movimientos positivos.
 */
val TIPOS_SIN_XP: List<String> = listOf(TIPO_NIVEL)

sealed class ResultadoRecompensa {
    data class Otorgada(val cantidad: Int) : ResultadoRecompensa()
    object YaOtorgada : ResultadoRecompensa()
    object TopeDiarioAlcanzado : ResultadoRecompensa()
    object SinSesion : ResultadoRecompensa()
    object Error : ResultadoRecompensa()
}

data class MovimientoEduCoins(
    val tipo: String,
    val cantidad: Int,
    val descripcion: String,
    val fechaMillis: Long
)
