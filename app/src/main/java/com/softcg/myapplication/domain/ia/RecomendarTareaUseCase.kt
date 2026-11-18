package com.softcg.myapplication.domain.ia

import android.util.Log
import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.network.RespuestaApi
import com.softcg.myapplication.domain.prediccion.CatalogoModelo
import com.softcg.myapplication.domain.prediccion.DatosPrediccion
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ExtraerDatosPrediccionUseCase
import com.softcg.myapplication.domain.prediccion.PredecirTareaUseCase
import com.softcg.myapplication.domain.prediccion.ResultadoPrediccion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Modelo de lenguaje que responde con el JSON del esquema de [GeminiRecomendaciones]. Se sustituye en los tests. */
interface GeneradorRecomendaciones {
    suspend fun generar(prompt: String): String
}

/** De dónde sale el método recomendado. */
sealed class ContextoPrediccion {
    /** El modelo predictivo respondió: el método es el de mayor confianza y Gemini no puede cambiarlo. */
    data class ConPrediccion(val prediccion: ResultadoPrediccion, val datos: DatosPrediccion) : ContextoPrediccion()

    /** Sin predicción: Gemini elige uno de los métodos del modelo y lo dice abiertamente. */
    data class SinPrediccion(val motivo: MotivoSinPrediccion, val faltantes: List<String>) : ContextoPrediccion()
}

enum class MotivoSinPrediccion(val apertura: String) {
    FALTAN_DATOS("No tengo suficiente información sobre ti, sin embargo te recomiendo"),
    MODELO_NO_DISPONIBLE("No pude consultar el modelo predictivo en este momento, sin embargo te recomiendo")
}

data class RecomendacionTarea(
    /** Nombre del catálogo, sin tildes. */
    val metodo: String,
    val contexto: ContextoPrediccion,
    /** Párrafo amigable: por qué ese método, con la confianza del modelo si la hay. */
    val intro: String,
    /** El método aplicado al tema de la tarea. */
    val consejo: String,
    val comoAplicar: List<String>,
    val tiempoEstimado: String,
    /** Solo fuentes comprobadas; puede estar vacía. */
    val fuentes: List<Fuente>
) {
    /** Confianza del modelo en el método, o null si no hubo predicción. */
    val confianza: String?
        get() = (contexto as? ContextoPrediccion.ConPrediccion)?.prediccion?.principal?.porcentaje
}

sealed class ResultadoRecomendacion {
    data class Exito(val recomendacion: RecomendacionTarea) : ResultadoRecomendacion()
    object SinConexion : ResultadoRecomendacion()
    object Error : ResultadoRecomendacion()
}

/**
 * Recomendación integral de una tarea, en tres pasos:
 * 1. El modelo predictivo (POST /predict) decide el método de estudio con los datos reales del estudiante.
 * 2. Gemini interpreta esa predicción: un párrafo con el método y su confianza, y cómo aplicarlo al tema de la
 *    tarea (su título). La respuesta se rechaza si cambia el método o no cita la confianza.
 * 3. La bibliografía que propone Gemini se comprueba una a una; lo que no existe no se muestra.
 * Si no hay datos suficientes para predecir, Gemini elige uno de los métodos del modelo y lo dice abiertamente.
 * Las respuestas se guardan en memoria mientras la tarea no cambie; "Generar otras" pasa [nueva] = true.
 */
@Singleton
class RecomendarTareaUseCase @Inject constructor(
    private val generador: GeneradorRecomendaciones,
    private val reloj: Reloj,
    private val extraer: ExtraerDatosPrediccionUseCase,
    private val predecir: PredecirTareaUseCase,
    private val verificador: VerificadorFuentes
) {
    private val cache = mutableMapOf<Tarea, RecomendacionTarea>()

    fun enCache(tarea: Tarea): RecomendacionTarea? = cache[tarea]

    suspend operator fun invoke(tarea: Tarea, nueva: Boolean = false): ResultadoRecomendacion {
        if (!nueva) cache[tarea]?.let { return ResultadoRecomendacion.Exito(it) }
        val contexto = contextoDe(tarea)
        val prompt = construirPrompt(tarea, contexto)

        var borrador: Borrador? = null
        for (intento in 1..INTENTOS) {
            val respuesta = try {
                generador.generar(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gemini no respondió", e)
                return if (e.causas().any { it is IOException }) ResultadoRecomendacion.SinConexion
                else ResultadoRecomendacion.Error
            }
            val leido = interpretar(respuesta)
            val problema = if (leido == null) "sin el formato esperado" else validar(leido, contexto)
            if (problema == null) {
                borrador = leido
                break
            }
            Log.w(TAG, "Respuesta de Gemini descartada (intento $intento, $problema): $respuesta")
        }
        val valido = borrador ?: return ResultadoRecomendacion.Error

        val fuentes = try {
            verificador.verificar(valido.fuentes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo comprobar la bibliografía", e)
            emptyList()
        }
        val recomendacion = RecomendacionTarea(
            valido.metodo, contexto, valido.intro, valido.consejo, valido.comoAplicar, valido.tiempoEstimado, fuentes
        )
        cache[tarea] = recomendacion
        return ResultadoRecomendacion.Exito(recomendacion)
    }

    /** Consulta el modelo predictivo con los datos reales; si no se puede, dice por qué. */
    internal suspend fun contextoDe(tarea: Tarea): ContextoPrediccion {
        val extraccion = try {
            extraer.deTarea(tarea)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron leer los datos del estudiante", e)
            return ContextoPrediccion.SinPrediccion(MotivoSinPrediccion.FALTAN_DATOS, emptyList())
        }
        val resultado = when (extraccion) {
            is Extraccion.Lista -> extraccion.resultado
            Extraccion.SinAsignatura -> return ContextoPrediccion.SinPrediccion(
                MotivoSinPrediccion.FALTAN_DATOS, listOf("La tarea no pertenece a una asignatura registrada")
            )
            Extraccion.SinSesion -> return ContextoPrediccion.SinPrediccion(MotivoSinPrediccion.FALTAN_DATOS, emptyList())
        }
        val datos = resultado.datos ?: return ContextoPrediccion.SinPrediccion(
            MotivoSinPrediccion.FALTAN_DATOS, resultado.estados.filterNot { it.listo }.map { it.parametro.titulo }
        )
        val prediccion = when (val respuesta = predecir(datos)) {
            is RespuestaApi.Http -> if (respuesta.exitosa) ResultadoPrediccion.desdeJson(respuesta.cuerpo) else {
                Log.w(TAG, "La API de predicción respondió ${respuesta.codigo}: ${respuesta.cuerpo.take(300)}")
                null
            }
            RespuestaApi.SinSesion -> null
            is RespuestaApi.ErrorRed -> {
                Log.w(TAG, "No se pudo conectar con la API de predicción: ${respuesta.mensaje}")
                null
            }
        }
        return if (prediccion != null) ContextoPrediccion.ConPrediccion(prediccion, datos)
        else ContextoPrediccion.SinPrediccion(MotivoSinPrediccion.MODELO_NO_DISPONIBLE, emptyList())
    }

    internal fun construirPrompt(tarea: Tarea, contexto: ContextoPrediccion): String {
        val tema = tarea.titulo.trim().ifBlank { "sin título" }
        val partes = mutableListOf(
            """
            Eres el tutor académico de EduPlanner, una app de planificación para estudiantes. Responde en español,
            con un tono cercano, claro y concreto, tuteando al estudiante.

            TAREA
            Tema (es el título de la tarea): $tema
            Descripción: ${tarea.descrip.ifBlank { "sin descripción" }}
            Asignatura: ${tarea.asignatura.ifBlank { "sin asignatura" }}
            Prioridad: ${prioridad(tarea.prioridad)}
            ${plazo(tarea)}
            """.trimIndent()
        )
        when (contexto) {
            is ContextoPrediccion.ConPrediccion -> {
                val p = contexto.prediccion
                val metodo = CatalogoModelo.etiqueta(p.principal.metodo)
                val otros = p.metodos.drop(1).joinToString(", ") { "${CatalogoModelo.etiqueta(it.metodo)} (${it.porcentaje})" }
                partes += buildList {
                    add("RESULTADO DEL MODELO PREDICTIVO DE EDUPLANNER (son hechos: no los cambies ni los discutas)")
                    add("- Método de estudio recomendado: $metodo, con ${p.principal.porcentaje} de confianza.")
                    if (otros.isNotEmpty()) add("- Otros métodos con menos confianza (solo como referencia, no los recomiendes): $otros.")
                    p.estadoPractica?.let { estado ->
                        add("- Estado del estudiante en el tema: $estado" +
                            (p.confianzaPractica?.let { " (${Math.round(it * 100)} % de confianza)." } ?: "."))
                    }
                    add("")
                    add("DATOS QUE EL ESTUDIANTE HA REGISTRADO EN EDUPLANNER (con ellos se hizo la predicción)")
                    resumenDatos(contexto.datos).forEach { add("- $it") }
                    add("")
                    add("INSTRUCCIONES")
                    add("1. \"metodo\": exactamente \"${p.principal.metodo}\".")
                    add("2. \"intro\": un párrafo amigable de 2 a 4 frases que diga que, según los datos que ha registrado en " +
                        "EduPlanner, el método de estudio recomendado para él es $metodo con un ${p.principal.porcentaje} de confianza. " +
                        "Escribe el porcentaje tal cual: \"${p.principal.porcentaje}\". Puedes mencionar uno o dos de sus datos. " +
                        "No menciones ningún otro método en este párrafo, salvo, si quieres, el que suele usar, sin recomendarlo.")
                    add("3. \"consejo\": cómo aprovechar $metodo para el tema de esta tarea (\"$tema\"), con ejemplos concretos " +
                        "del contenido. ${consejoSegunEstado(p.estadoPractica)}".trim())
                    add("4. \"comoAplicar\": de 3 a 5 pasos prácticos, en orden, para estudiar el contenido de esta tarea con $metodo.")
                    add("Nunca recomiendes un método distinto de $metodo como principal.")
                }.joinToString("\n")
            }
            is ContextoPrediccion.SinPrediccion -> {
                val opciones = CatalogoModelo.METODOS_RECOMENDABLES.joinToString(", ") { "\"$it\"" }
                val porque = when (contexto.motivo) {
                    MotivoSinPrediccion.FALTAN_DATOS ->
                        "El estudiante aún no ha registrado en EduPlanner los datos suficientes para usar el modelo predictivo" +
                            if (contexto.faltantes.isNotEmpty()) " (faltan: ${contexto.faltantes.joinToString(", ")})." else "."
                    MotivoSinPrediccion.MODELO_NO_DISPONIBLE -> "El modelo predictivo de EduPlanner no está disponible en este momento."
                }
                partes += """
                    SIN PREDICCIÓN
                    $porque

                    INSTRUCCIONES
                    1. "metodo": elige UNO de estos métodos, el que mejor sirva para el contenido de esta tarea: $opciones.
                    2. "intro": un párrafo amigable de 2 a 4 frases que empiece exactamente con "${contexto.motivo.apertura}",
                       siga con el método elegido y explique por qué encaja con esta tarea.${
                        if (contexto.motivo == MotivoSinPrediccion.FALTAN_DATOS)
                            " Invítalo a registrar sus datos en EduPlanner (notas, asistencia, tiempo de estudio...) para recibir recomendaciones personalizadas."
                        else ""
                    } No escribas porcentajes de confianza.
                    3. "consejo": cómo aprovechar ese método para el tema de esta tarea ("$tema"), con ejemplos concretos del contenido.
                    4. "comoAplicar": de 3 a 5 pasos prácticos, en orden, para estudiar el contenido de esta tarea con ese método.
                """.trimIndent()
            }
        }
        partes += """
            5. "tiempoEstimado": tiempo total aproximado de trabajo, por ejemplo "2 horas" o "3 sesiones de 45 minutos".
            6. "fuentes": de 3 a 6 recursos REALES para profundizar en el tema "$tema":
               - tipo "libro": libros académicos o manuales universitarios reconocidos, con el "titulo" exacto y el "autor"
                 (nombre y apellido del primer autor); "url" vacío.
               - tipo "web": páginas de universidades, enciclopedias como Wikipedia, Khan Academy, documentación oficial u
                 otros sitios educativos confiables, con la "url" completa que empiece por https; en "autor" la institución.
               Incluye solo fuentes que sepas con certeza que existen. No inventes títulos, autores ni enlaces: si dudas,
               no la incluyas. Cada fuente se verificará automáticamente y las que no existan se descartarán.
        """.trimIndent()
        return partes.joinToString("\n\n")
    }

    /** Lo que respondió Gemini, antes de comprobar las fuentes. */
    internal data class Borrador(
        val metodo: String,
        val intro: String,
        val consejo: String,
        val comoAplicar: List<String>,
        val tiempoEstimado: String,
        val fuentes: List<FuentePropuesta>
    )

    internal fun interpretar(json: String): Borrador? = runCatching {
        // Por si el modelo envuelve el JSON en un bloque de código
        val limpio = json.substringAfter('{', "").substringBeforeLast('}', "")
        val objeto = JSONObject("{$limpio}")
        val pasos = objeto.optJSONArray("comoAplicar")
        val fuentes = objeto.optJSONArray("fuentes")
        Borrador(
            metodo = objeto.optString("metodo").trim(),
            intro = objeto.optString("intro").trim(),
            consejo = objeto.optString("consejo").trim(),
            comoAplicar = (0 until (pasos?.length() ?: 0)).map { pasos!!.optString(it).trim() }.filter { it.isNotEmpty() },
            tiempoEstimado = objeto.optString("tiempoEstimado").trim(),
            fuentes = (0 until (fuentes?.length() ?: 0)).mapNotNull { i ->
                val fuente = fuentes!!.optJSONObject(i) ?: return@mapNotNull null
                val tipo = when (fuente.optString("tipo").trim().lowercase()) {
                    "libro" -> TipoFuente.LIBRO
                    "web" -> TipoFuente.WEB
                    else -> return@mapNotNull null
                }
                FuentePropuesta(tipo, fuente.optString("titulo").trim(), fuente.optString("autor").trim(), fuente.optString("url").trim())
                    .takeIf { it.titulo.isNotEmpty() }
            }
        )
    }.getOrNull()?.takeIf { it.intro.isNotEmpty() && it.consejo.isNotEmpty() && it.comoAplicar.isNotEmpty() }

    /** null si la respuesta es coherente con la predicción; si no, el motivo para descartarla. */
    internal fun validar(borrador: Borrador, contexto: ContextoPrediccion): String? {
        val intro = CatalogoModelo.normalizar(borrador.intro)
        // El que suele usar sí se puede nombrar ("aunque sueles usar Pomodoro..."): es un dato suyo, no una recomendación
        val habitual = (contexto as? ContextoPrediccion.ConPrediccion)?.datos?.studyMethod
        val otrosMetodos = CatalogoModelo.METODOS_RECOMENDABLES.filter { it != borrador.metodo && it != habitual }
        if (otrosMetodos.any { intro.contains(CatalogoModelo.normalizar(it)) }) return "la introducción menciona otro método"
        return when (contexto) {
            is ContextoPrediccion.ConPrediccion -> {
                val principal = contexto.prediccion.principal
                val numero = Math.round(principal.confianza * 100)
                when {
                    borrador.metodo != principal.metodo -> "cambió el método ${principal.metodo} por ${borrador.metodo}"
                    !Regex("(?<!\\d)$numero\\s?(%|por ciento)").containsMatchIn(intro) -> "no cita la confianza ${principal.porcentaje}"
                    else -> null
                }
            }
            is ContextoPrediccion.SinPrediccion -> when {
                borrador.metodo !in CatalogoModelo.METODOS_RECOMENDABLES -> "método fuera del catálogo: ${borrador.metodo}"
                !intro.startsWith(CatalogoModelo.normalizar(contexto.motivo.apertura)) -> "no empieza aclarando que no hay predicción"
                Regex("\\d\\s?%").containsMatchIn(intro) -> "inventa un porcentaje"
                else -> null
            }
        }
    }

    private fun resumenDatos(datos: DatosPrediccion): List<String> = listOf(
        "Promedio de notas: ${decimal(datos.gradeAvg)} / 10",
        "Asistencia a clase: ${(datos.attendanceAvg * 10).roundToInt()} %",
        "Tiempo de estudio: ${decimal(datos.intensityHours)} horas por semana",
        "Interés en la asignatura: ${decimal(datos.interestLevel)} / 10",
        "Dificultad percibida: ${decimal(datos.difficultyPerception)} / 10",
        "Método que suele usar: ${CatalogoModelo.etiqueta(datos.studyMethod)}"
    )

    private fun consejoSegunEstado(estado: String?) = when (estado) {
        "Dominada" -> "Como el modelo indica que ya domina el tema, orienta el consejo a consolidarlo y repasarlo."
        "Reforzar" -> "Como el modelo indica que debe reforzar el tema, orienta el consejo a afianzar lo que le cuesta."
        "Debe practicar" -> "Como el modelo indica que debe practicar el tema, orienta el consejo a la práctica frecuente."
        else -> ""
    }

    private fun prioridad(valor: Int) = when (valor) {
        1 -> "alta"
        2 -> "media"
        3 -> "baja"
        else -> "sin definir"
    }

    private fun plazo(tarea: Tarea): String {
        val hoy = reloj.ahora().toLocalDate()
        val fecha = runCatching { LocalDate.parse(tarea.fecha) }.getOrNull()
        return when {
            tarea.completada -> "La tarea ya está ${if (tarea.calificacion != null) "calificada (nota ${tarea.calificacion})" else "entregada"}: orienta el consejo a repasar lo aprendido."
            fecha == null -> "No tiene fecha de entrega."
            else -> when (val dias = ChronoUnit.DAYS.between(hoy, fecha)) {
                0L -> "Se entrega hoy."
                1L -> "Se entrega mañana."
                in 2L..Long.MAX_VALUE -> "Faltan $dias días para la entrega (${tarea.fecha})."
                else -> "La fecha de entrega (${tarea.fecha}) ya pasó hace ${-dias} días."
            }
        }
    }

    private fun decimal(valor: Double) = String.format(Locale("es", "CO"), "%.1f", valor)

    private fun Throwable.causas(): Sequence<Throwable> = generateSequence(this) { it.cause }

    private companion object {
        const val TAG = "RecomendarTarea"

        /** Si Gemini contradice la predicción se le pide otra vez, una sola. */
        const val INTENTOS = 2
    }
}
