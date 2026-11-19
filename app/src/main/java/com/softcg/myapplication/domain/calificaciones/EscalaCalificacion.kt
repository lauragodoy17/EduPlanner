package com.softcg.myapplication.domain.calificaciones

import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.domain.prediccion.ReglasExtraccion
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tipos de escala de notas. El estudiante elige entre [ELEGIBLES] al crear la asignatura; [PERSONALIZADA] solo existe
 * para leer asignaturas antiguas con un rango propio (p. ej. "Otro" de 1 a 7) y sus notas.
 */
enum class TipoEscala(
    /** Lo que se guarda en `tipo_calificacion` (no cambiarlo: hay asignaturas guardadas con estos textos). */
    val etiqueta: String,
    /** Texto corto del chip. */
    val corto: String
) {
    CERO_A_DIEZ("Numérico (0-10)", "0 a 10"),
    UNO_A_CINCO("Numérico (1-5)", "1 a 5"),
    PORCENTAJE("Porcentaje (0-100%)", "0 a 100 %"),
    LETRAS("Letras (A-F)", "Letras A-F"),
    APROBADO("Aprobado/Reprobado", "Aprobado / Reprobado"),
    PERSONALIZADA("Personalizada", "Otra escala");

    companion object {
        /** Las escalas que ofrece la app; no se pueden inventar otras. */
        val ELEGIBLES: List<TipoEscala> = entries.filter { it != PERSONALIZADA }
    }
}

/**
 * Escala de notas de una asignatura. La nota siempre se guarda como número dentro de [min]..[max]: las letras van
 * de F = 0 a A = 4 y aprobado/reprobado es 1/0. Para el modelo predictivo se lleva a 0-10 con [a10].
 */
data class EscalaCalificacion(val tipo: TipoEscala, val min: Float, val max: Float) {

    /** Valores posibles cuando la escala no es continua (letras, aprobado/reprobado); null si es numérica. */
    val opciones: List<Pair<Float, String>>?
        get() = when (tipo) {
            TipoEscala.LETRAS -> LETRAS.mapIndexed { i, letra -> (LETRAS.size - 1 - i).toFloat() to letra }
            TipoEscala.APROBADO -> listOf(1f to "Aprobado", 0f to "Reprobado")
            else -> null
        }

    /** Incremento del deslizador: un decimal en escalas cortas, enteros en las largas. */
    val paso: Float
        get() = when (tipo) {
            TipoEscala.CERO_A_DIEZ, TipoEscala.UNO_A_CINCO -> 0.1f
            TipoEscala.PERSONALIZADA -> if (max - min <= MAX_RANGO_DECIMAL) 0.1f else 1f
            else -> 1f
        }

    /**
     * La nota en la escala 0-10 del modelo (`grade_avg`).
     * - Numéricas: proporción sobre la nota máxima, `nota / máximo × 10`, la equivalencia usual entre escalas
     *   (3,0 de 5 = 6,0 de 10 = 60 %). Así 0-10, 1-5 y 0-100 % dan lo mismo para el mismo rendimiento.
     * - Letras y aprobado/reprobado no son números: cada opción vale el punto medio de su banda habitual en
     *   porcentaje (A 90-100 % → 9,5; B → 8,5; C → 7,5; D → 6,5; F 0-59 % → 3,0; aprobado 60-100 % → 8,0;
     *   reprobado 0-59 % → 3,0). Tratarlas como un rango lineal (A = 10, F = 0) exageraría las diferencias.
     * El valor se lee desde su texto decimal (4,3 es 4,3 y no 4,30000019) para no arrastrar el error del Float.
     */
    fun a10(valor: Float): Double {
        val exacto = valor.toString().toDouble()
        val resultado = when (tipo) {
            TipoEscala.LETRAS -> EQUIVALENCIA_LETRAS[ajustar(valor).roundToInt()] ?: 0.0
            TipoEscala.APROBADO -> if (ajustar(valor) >= 1f) APROBADO_10 else REPROBADO_10
            else -> exacto / max.toString().toDouble() * 10.0
        }
        return resultado.coerceIn(0.0, 10.0)
    }

    /** Lo que se guarda en `tipo_calificacion` de la asignatura y en el tipo de cada nota manual. */
    val etiquetaGuardada: String
        get() = if (tipo == TipoEscala.PERSONALIZADA) "${tipo.etiqueta} (${numero(min)}-${numero(max)})" else tipo.etiqueta

    /** "Notas de 1 a 5 con un decimal, por ejemplo 4,5". */
    val descripcion: String
        get() = when (tipo) {
            TipoEscala.LETRAS -> "Notas con letra: A, B, C, D o F"
            TipoEscala.APROBADO -> "Solo aprobado o reprobado"
            TipoEscala.PORCENTAJE -> "Notas de 0 a 100 %, por ejemplo 85 %"
            else -> if (paso < 1f) "Notas de ${numero(min)} a ${numero(max)} con un decimal, por ejemplo ${numero(ajustar(min + (max - min) * 0.75f))}"
            else "Notas de ${numero(min)} a ${numero(max)} en números enteros"
        }

    fun contiene(valor: Float): Boolean {
        val opciones = opciones
        if (opciones != null) return opciones.any { abs(it.first - valor) < TOLERANCIA }
        return valor >= min - TOLERANCIA && valor <= max + TOLERANCIA
    }

    /** El valor válido más cercano: dentro del rango y en un múltiplo del paso (o la opción más cercana). */
    fun ajustar(valor: Float): Float {
        opciones?.let { lista -> return lista.minByOrNull { abs(it.first - valor) }!!.first }
        val limitado = valor.coerceIn(min, max)
        val pasos = ((limitado - min) / paso).roundToInt()
        val ajustado = min + pasos * paso
        return (Math.round(ajustado * 10f) / 10f).coerceIn(min, max)
    }

    /** Valor con que empieza el deslizador si la nota es nueva: la mitad de la escala. */
    val valorInicial: Float get() = ajustar(min + (max - min) / 2f)

    /** "4,5", "85 %", "A", "Aprobado". */
    fun formatear(valor: Float): String {
        opciones?.let { lista -> return lista.minByOrNull { abs(it.first - valor) }!!.second }
        val texto = numero(valor)
        return if (tipo == TipoEscala.PORCENTAJE) "$texto %" else texto
    }

    companion object {
        private const val TOLERANCIA = 0.001f
        private val LETRAS = listOf("A", "B", "C", "D", "F")

        /** Más amplia que esto, una escala antigua personalizada va en enteros. */
        private const val MAX_RANGO_DECIMAL = 20f

        /** Valor 0-10 de cada letra guardada (F = 0 ... A = 4): punto medio de su banda en porcentaje. */
        private val EQUIVALENCIA_LETRAS = mapOf(4 to 9.5, 3 to 8.5, 2 to 7.5, 1 to 6.5, 0 to 3.0)
        private const val APROBADO_10 = 8.0
        private const val REPROBADO_10 = 3.0

        fun de(tipo: TipoEscala): EscalaCalificacion = when (tipo) {
            TipoEscala.CERO_A_DIEZ -> EscalaCalificacion(tipo, 0f, 10f)
            TipoEscala.UNO_A_CINCO -> EscalaCalificacion(tipo, 1f, 5f)
            TipoEscala.PORCENTAJE -> EscalaCalificacion(tipo, 0f, 100f)
            TipoEscala.LETRAS -> EscalaCalificacion(tipo, 0f, 4f)
            TipoEscala.APROBADO -> EscalaCalificacion(tipo, 0f, 1f)
            TipoEscala.PERSONALIZADA -> EscalaCalificacion(tipo, 0f, 100f)
        }

        /** Las escalas que se pueden elegir. */
        val PREDEFINIDAS: List<EscalaCalificacion> = TipoEscala.ELEGIBLES.map(::de)

        /**
         * Escala de un texto guardado: "Numérico (1-5)", "Letras (A-F)", "de 1 a 7" (antiguo)... Sirve para el tipo de
         * la asignatura y para el de las notas antiguas ("Numerico (1-5)"). null si el texto no indica una escala ("Tarea").
         */
        fun deTexto(texto: String?): EscalaCalificacion? {
            if (texto.isNullOrBlank()) return null
            val normal = CatalogoModelo.normalizar(texto)
            if ("letra" in normal) return de(TipoEscala.LETRAS)
            if ("aprobado" in normal || "reprobado" in normal) return de(TipoEscala.APROBADO)
            val rango = ReglasExtraccion.escalaDeTexto(texto) ?: return null
            return deRango(rango.min, rango.max)
        }

        /** Un rango numérico es una de las escalas de la app si coincide; si no (solo en datos antiguos), personalizada. */
        private fun deRango(min: Float, max: Float): EscalaCalificacion? {
            PREDEFINIDAS.firstOrNull { it.opciones == null && it.min == min && it.max == max }?.let { return it }
            return if (max > min && min >= 0f) EscalaCalificacion(TipoEscala.PERSONALIZADA, min, max) else null
        }

        /**
         * Escala de una asignatura. Las creadas con el selector guardan tipo y rango; en las antiguas manda el rango que
         * indicó el estudiante y, si no hay, el del tipo ("Numérico (0-10)"). Una antigua de letras o aprobado/reprobado
         * sin rango no tiene escala fiable (sus notas se escribían como números libres): null, para preguntarla.
         */
        fun deAsignatura(tipoCalificacion: String, escalaMin: Float?, escalaMax: Float?): EscalaCalificacion? {
            val porTexto = deTexto(tipoCalificacion)
            if (escalaMin != null && escalaMax != null && escalaMax > escalaMin) {
                if (porTexto?.opciones != null && porTexto.min == escalaMin && porTexto.max == escalaMax) return porTexto
                return deRango(escalaMin, escalaMax)
            }
            return porTexto?.takeIf { it.opciones == null }
        }

        fun de(asignatura: Asignatura): EscalaCalificacion? =
            deAsignatura(asignatura.tipoCalificacion, asignatura.escalaMin, asignatura.escalaMax)

        /** "4", "4,5". */
        fun numero(valor: Float): String {
            val redondeado = Math.round(valor * 10f) / 10f
            return if (redondeado % 1f == 0f) redondeado.toInt().toString() else redondeado.toString().replace('.', ',')
        }

        /** Texto de una nota guardada: con su escala si se conoce; si no, el número tal cual. */
        fun texto(valor: Float, escala: EscalaCalificacion?): String = escala?.formatear(valor) ?: numero(valor)
    }
}
