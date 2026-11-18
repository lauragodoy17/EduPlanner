package com.softcg.myapplication.domain.prediccion

import org.json.JSONObject

data class MetodoPredicho(
    /** Nombre del catálogo, sin tildes ("Metodo Feynman"). */
    val metodo: String,
    /** Probabilidad que le da el modelo, de 0 a 1. */
    val confianza: Double
) {
    /** "34 %": así se le muestra al estudiante y así debe citarlo Gemini. */
    val porcentaje: String get() = "${Math.round(confianza * 100)} %"
}

/**
 * Lo que respondió POST /predict, ya leído. Formato (api.md de modelos_exportados):
 * `{"prediccion": {..., "practice_label", "practice_confianza", "metodo_top1", "metodo_top1_prob", ...}, "avisos": [...]}`
 */
data class ResultadoPrediccion(
    /** "Debe practicar", "Reforzar" o "Dominada". */
    val estadoPractica: String?,
    val confianzaPractica: Double?,
    /** De mayor a menor confianza; nunca vacía. */
    val metodos: List<MetodoPredicho>,
    val avisos: List<String>
) {
    /** El método con la confianza más alta: el que se recomienda. */
    val principal: MetodoPredicho get() = metodos.first()

    companion object {
        /** null si la respuesta no trae al menos un método del catálogo con su probabilidad. */
        fun desdeJson(texto: String): ResultadoPrediccion? = runCatching<ResultadoPrediccion?> {
            val raiz = JSONObject(texto)
            val prediccion = raiz.getJSONObject("prediccion")
            val metodos = generateSequence(1) { it + 1 }
                .takeWhile { prediccion.has("metodo_top$it") }
                .mapNotNull { i ->
                    val metodo = prediccion.optString("metodo_top$i")
                    val prob = prediccion.optDouble("metodo_top${i}_prob")
                    if (metodo in CatalogoModelo.METODOS_RECOMENDABLES && !prob.isNaN() && prob in 0.0..1.0) MetodoPredicho(metodo, prob)
                    else null
                }
                .sortedByDescending { it.confianza }
                .toList()
            val avisos = raiz.optJSONArray("avisos")
            if (metodos.isEmpty()) null else ResultadoPrediccion(
                estadoPractica = prediccion.optString("practice_label").takeIf { it.isNotBlank() },
                confianzaPractica = prediccion.optDouble("practice_confianza").takeUnless { it.isNaN() },
                metodos = metodos,
                avisos = (0 until (avisos?.length() ?: 0)).map { avisos!!.optString(it) }.filter { it.isNotBlank() }
            )
        }.getOrNull()
    }
}
