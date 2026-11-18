package com.softcg.myapplication.data.network

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.softcg.myapplication.domain.ia.GeneradorRecomendaciones
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini a través de Firebase AI Logic con la Gemini Developer API (habilitada en la consola, sin plan de pago).
 * La clave de la API vive en Firebase: la app no guarda ninguna.
 */
@Singleton
class GeminiRecomendaciones @Inject constructor() : GeneradorRecomendaciones {

    private val modelo by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODELO,
            generationConfig = generationConfig {
                // Baja: tiene que seguir la predicción al pie de la letra, no improvisar
                temperature = 0.4f
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    mapOf(
                        // Solo los métodos que el modelo predictivo puede recomendar
                        "metodo" to Schema.enumeration(CatalogoModelo.METODOS_RECOMENDABLES),
                        "intro" to Schema.string(),
                        "consejo" to Schema.string(),
                        "comoAplicar" to Schema.array(Schema.string()),
                        "tiempoEstimado" to Schema.string(),
                        "fuentes" to Schema.array(
                            Schema.obj(
                                mapOf(
                                    "tipo" to Schema.enumeration(listOf("libro", "web")),
                                    "titulo" to Schema.string(),
                                    "autor" to Schema.string(),
                                    "url" to Schema.string()
                                )
                            )
                        )
                    )
                )
            },
            requestOptions = RequestOptions(timeoutInMillis = ESPERA_MAXIMA_MS)
        )
    }

    override suspend fun generar(prompt: String): String =
        modelo.generateContent(prompt).text ?: error("Gemini no devolvió texto")

    companion object {
        /** Rápido, estable y sin facturación en la Gemini Developer API (nov. 2026); gemini-3.8-flash daba "high demand". */
        const val MODELO = "gemini-3.5-flash-lite"

        /** Sin límite propio el SDK espera 180 s antes de rendirse. */
        const val ESPERA_MAXIMA_MS = 45_000L
    }
}
