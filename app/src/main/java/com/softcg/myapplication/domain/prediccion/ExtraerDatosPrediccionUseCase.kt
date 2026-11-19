package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.PercepcionesRepository
import com.softcg.myapplication.data.database.entities.PercepcionEntity
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Lo que se le muestra al estudiante en "Datos para predicciones". Agrupa los 8 campos de POST /predict:
 * la asistencia no aparece (se supone del 100 % hasta tener clases respondidas) y el área y el tema solo
 * aparecen cuando no se pudieron obtener solos.
 */
enum class ItemDato(val titulo: String) {
    AREA("Área de la asignatura"),
    TEMA("Tema de la tarea"),
    NOTAS("Promedio de notas"),
    ESTUDIO("Tiempo y método de estudio"),
    PERCEPCION("Interés y dificultad")
}

/** Qué hoja abre el botón del ítem. Sin acción = solo hay que esperar o registrar en otra pantalla (p. ej. notas). */
enum class AccionDato(val texto: String) {
    AREA("Elegir área"),
    ESCALA("Indicar escala"),
    ESTUDIO("Completar"),
    PERCEPCION("Completar")
}

data class EstadoDato(
    val item: ItemDato,
    val listo: Boolean,
    /** Valor legible ("86 %"), vacío si aún no hay. */
    val valor: String,
    /** Qué falta o de dónde sale el valor. */
    val detalle: String,
    val accion: AccionDato? = null
)

data class ResultadoExtraccion(
    val asignatura: Asignatura,
    val tarea: Tarea?,
    val estados: List<EstadoDato>,
    /** Solo cuando todos los datos están listos y hay una tarea con tema: lo que se envía a la API. */
    val datos: DatosPrediccion?,
    /** Valores reales pero fuera del rango de entrenamiento del modelo. */
    val avisos: List<String>,
    /** Días desde la última respuesta de interés (null si nunca respondió): se recuerda actualizarla cada mes. */
    val diasDesdeInteres: Long? = null
) {
    val listos: Int get() = estados.count { it.listo }
    val total: Int get() = estados.size
    val completo: Boolean get() = estados.all { it.listo }
}

sealed class Extraccion {
    data class Lista(val resultado: ResultadoExtraccion) : Extraccion()
    /** La tarea no corresponde a ninguna asignatura registrada: no hay de dónde sacar sus datos. */
    object SinAsignatura : Extraccion()
    object SinSesion : Extraccion()
}

/**
 * Sistema de extracción automática: consulta los registros del estudiante, calcula cada parámetro del modelo,
 * comprueba que haya datos suficientes y construye el [DatosPrediccion] que se envía a la API.
 * Nunca inventa un dato que el estudiante debe aportar: si falta, el resultado lo dice y no se construye la petición.
 * La única excepción es la asistencia, que se supone del 100 % mientras no haya [ReglasExtraccion.MIN_CLASES] clases.
 *
 * Con `tarea = null` evalúa solo la asignatura (panel de progreso): el tema se elige en cada tarea.
 */
class ExtraerDatosPrediccionUseCase @Inject constructor(
    private val asignaturas: AsignaturasRepository,
    private val calificaciones: CalificacionesRespository,
    private val gamificacion: GamificacionRepository,
    private val percepciones: PercepcionesRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {

    suspend fun deTarea(tarea: Tarea): Extraccion {
        val asignaturaId = tarea.asignaturaId ?: asignaturas.idPorNombre(tarea.asignatura) ?: return Extraccion.SinAsignatura
        return deAsignatura(asignaturaId, tarea)
    }

    suspend fun deAsignatura(asignaturaId: Int, tarea: Tarea? = null): Extraccion {
        val uid = sesion.uid() ?: return Extraccion.SinSesion
        val asignatura = asignaturas.getAsignatura(asignaturaId) ?: return Extraccion.SinAsignatura
        val ahora = reloj.ahora()
        val ahoraMillis = ahora.toInstant().toEpochMilli()

        val suyas = percepciones.deAsignatura(asignaturaId)
        val sesiones = gamificacion.sesionesEstudio(uid).filter { it.esDe(asignatura) }
        val asistencias = gamificacion.asistencias(uid).filter { it.asignaturaId == asignaturaId }
        val notas = calificaciones.deAsignatura(asignaturaId, asignatura.nombre)

        val avisos = mutableListOf<String>()
        val estados = mutableListOf<EstadoDato>()

        // ÁREA: solo se muestra si no se pudo detectar por el nombre ni la eligió el estudiante
        val area = asignatura.areaModelo?.takeIf { CatalogoModelo.esArea(it) }
        if (area == null) {
            val deteccion = CatalogoModelo.detectarArea(asignatura.nombre)
            estados += EstadoDato(ItemDato.AREA, false, "", TextosArea.pregunta(asignatura.nombre, deteccion), AccionDato.AREA)
        }

        // TEMA (solo al predecir una tarea): sale del título; solo se muestra si la tarea no tiene título
        val tema = if (tarea != null && area != null) temaDe(tarea, area) else null
        if (tarea != null && area != null) {
            if (tema == null) {
                estados += EstadoDato(ItemDato.TEMA, false, "", "Ponle un título a la tarea para saber de qué tema es")
            } else if (!CatalogoModelo.esTema(area, tema)) {
                avisos += "El modelo no tiene «$tema» entre sus temas de ${CatalogoModelo.etiqueta(area)}: predice con el resto de tus datos"
            }
        }

        // NOTAS: no se registran desde aquí, solo se indica que faltan
        val medidaNotas = ReglasExtraccion.notas(notas.map { nota ->
            nota.valor to ReglasExtraccion.escalaDe(nota.tipo, asignatura.escalaMin, asignatura.escalaMax, asignatura.tipoCalificacion)
        })
        estados += when {
            medidaNotas.usadas >= ReglasExtraccion.MIN_NOTAS -> EstadoDato(
                ItemDato.NOTAS, true, "${decimal(medidaNotas.promedio10!!)} / 10",
                "${medidaNotas.usadas} notas" + if (medidaNotas.fueraDeEscala > 0) " · ${medidaNotas.fueraDeEscala} fuera de la escala, no se usan" else ""
            )
            // Hay notas pero no se pueden pasar a 0-10 sin saber la escala
            medidaNotas.sinEscala > 0 -> EstadoDato(
                ItemDato.NOTAS, false, "", "Indica la escala de notas de la asignatura para usar tus ${medidaNotas.sinEscala + medidaNotas.usadas} notas",
                AccionDato.ESCALA
            )
            else -> EstadoDato(
                ItemDato.NOTAS, false, "",
                "Registra más notas en esta asignatura para poder realizar la recomendación (${medidaNotas.usadas} de ${ReglasExtraccion.MIN_NOTAS})"
            )
        }

        // ASISTENCIA: no se le pide al estudiante; hasta tener clases respondidas se supone del 100 %
        val medidaAsistencia = ReglasExtraccion.asistencia(asistencias, ahora.toLocalDate())

        // Tiempo y forma de estudio: un solo ítem; la hoja los pide por separado
        val medidaEstudio = ReglasExtraccion.intensidad(sesiones, ahoraMillis, ahora.zone)
        val horas = ReglasExtraccion.horasParaModelo(medidaEstudio, ReglasExtraccion.horasDeclaradas(suyas))
        val metodo = ReglasExtraccion.metodo(sesiones, suyas)
        estados += estadoEstudio(horas, metodo, medidaEstudio)
        if (horas != null && horas.first !in CatalogoModelo.RANGO_INTENSIDAD) {
            avisos += "Tu tiempo de estudio (${decimal(horas.first)} h/semana) está fuera del rango con que se entrenó el modelo (1 a 6 h)"
        }

        // INTERÉS Y DIFICULTAD: un solo ítem; la hoja los pide por separado
        val interes = ReglasExtraccion.interes(suyas)
        val interesVigente = interes?.takeIf { ReglasExtraccion.interesVigente(it, ahoraMillis) }
        val dificultad = ReglasExtraccion.dificultad(suyas, tarea?.tema)
        estados += estadoPercepcion(interes, interesVigente != null, dificultad)

        val completo = estados.all { it.listo }
        val datos = if (completo && tarea != null) {
            DatosPrediccion(
                attendanceAvg = redondear(ReglasExtraccion.asistenciaModelo(medidaAsistencia)),
                intensityHours = redondear(horas!!.first),
                gradeAvg = redondear(medidaNotas.promedio10!!),
                difficultyPerception = redondear(dificultad!!.first),
                interestLevel = redondear(interesVigente!!.valor!!.toDouble()),
                subjectName = area!!,
                subjectTopic = tema!!,
                studyMethod = metodo!!,
                asistenciaSupuesta = !ReglasExtraccion.asistenciaMedida(medidaAsistencia)
            )
        } else null
        // La asistencia supuesta (100 %) queda fuera del rango de entrenamiento: solo se avisa de la medida
        if (datos != null && ReglasExtraccion.asistenciaMedida(medidaAsistencia) && datos.attendanceAvg !in CatalogoModelo.RANGO_ASISTENCIA) {
            avisos += "Tu asistencia está fuera del rango con que se entrenó el modelo: la predicción puede ser menos precisa"
        }

        val diasDesdeInteres = interes?.let { (ahoraMillis - it.fechaMillis) / (24 * 60 * 60_000L) }
        return Extraccion.Lista(ResultadoExtraccion(asignatura, tarea, estados, datos, avisos, diasDesdeInteres))
    }

    /** El método que el estudiante suele usar en la asignatura, para proponerlo al empezar a estudiar. */
    suspend fun metodoHabitual(asignaturaId: Int): String? {
        val uid = sesion.uid() ?: return null
        val asignatura = asignaturas.getAsignatura(asignaturaId) ?: return null
        val sesiones = gamificacion.sesionesEstudio(uid).filter { it.esDe(asignatura) }
        return ReglasExtraccion.metodo(sesiones, percepciones.deAsignatura(asignaturaId))
    }

    /**
     * El tema que se envía al modelo: el que la tarea ya tenga del catálogo (tareas antiguas), el del catálogo
     * que se reconozca en el título, o el propio título. Un tema fuera del catálogo la API lo acepta y lo
     * ignora (one-hot con handle_unknown="ignore"): la predicción usa entonces los otros 7 datos reales.
     */
    private fun temaDe(tarea: Tarea, area: String): String? {
        if (CatalogoModelo.esTema(area, tarea.tema)) return tarea.tema
        val titulo = tarea.titulo.trim().ifEmpty { return null }
        return CatalogoModelo.temaEnTexto(area, titulo) ?: titulo.take(MAX_TEXTO_API)
    }

    private fun estadoEstudio(horas: Pair<Double, OrigenHoras>?, metodo: String?, medida: MedidaIntensidad?): EstadoDato {
        val valor = listOfNotNull(
            horas?.let { "${decimal(it.first)} h/semana" }, metodo?.let(CatalogoModelo::etiqueta)
        ).joinToString(" · ")
        if (horas != null && metodo != null) {
            val origen = if (horas.second == OrigenHoras.SESIONES) "Según tus sesiones de estudio" else "Según lo que nos contaste"
            return EstadoDato(ItemDato.ESTUDIO, true, valor, origen)
        }
        val detalle = when {
            horas == null && metodo == null -> "Cuéntanos cuántas horas a la semana estudias esta asignatura y con qué método"
            horas == null -> "Falta cuántas horas a la semana estudias esta asignatura" +
                if (medida != null) " (llevas ${medida.diasDeHistorial} de ${ReglasExtraccion.MIN_DIAS_HISTORIAL_ESTUDIO} días de sesiones)" else ""
            else -> "Falta con qué método estudias esta asignatura"
        }
        return EstadoDato(ItemDato.ESTUDIO, false, valor, detalle, AccionDato.ESTUDIO)
    }

    private fun estadoPercepcion(interes: PercepcionEntity?, interesVigente: Boolean, dificultad: Pair<Double, Int>?): EstadoDato {
        val valor = listOfNotNull(
            interes?.valor?.let { "Interés ${decimal(it.toDouble())}" }, dificultad?.let { "Dificultad ${decimal(it.first)}" }
        ).joinToString(" · ")
        if (interesVigente && dificultad != null) return EstadoDato(ItemDato.PERCEPCION, true, valor, "Tus últimas respuestas (de 0 a 10)")
        val detalle = when {
            interes == null && dificultad == null -> "Cuéntanos cuánto te interesa esta asignatura y qué tan difícil te parece"
            interes == null -> "Falta cuánto te interesa esta asignatura"
            !interesVigente -> "Tu respuesta de interés tiene más de ${ReglasExtraccion.VIGENCIA_INTERES_DIAS} días: actualízala" +
                if (dificultad == null) " y cuéntanos qué tan difícil te parece" else ""
            else -> "Falta qué tan difícil te parece esta asignatura"
        }
        return EstadoDato(ItemDato.PERCEPCION, false, valor, detalle, AccionDato.PERCEPCION)
    }

    /** Sesiones enlazadas por id, o las antiguas que solo guardaron el nombre. */
    private fun SesionEnfoqueEntity.esDe(materia: Asignatura) =
        asignaturaId == materia.id || (asignaturaId == null && this.asignatura.trim() == materia.nombre.trim())

    private fun redondear(valor: Double) = (valor * 100).roundToInt() / 100.0

    private fun decimal(valor: Double) = String.format(Locale("es", "CO"), "%.1f", valor)


    private companion object {
        /** La API rechaza (422) textos de más de 100 caracteres. */
        const val MAX_TEXTO_API = 100
    }
}
