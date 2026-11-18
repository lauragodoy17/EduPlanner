package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.PercepcionesRepository
import com.softcg.myapplication.data.database.entities.SesionEnfoqueEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.gamificacion.AsistenciaReglas
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

/** Los 8 campos que exige POST /predict, en el orden en que se muestran al estudiante. */
enum class ParametroModelo(val titulo: String, val campoApi: String) {
    AREA("Área de la asignatura", "subject_name"),
    TEMA("Tema de la tarea", "subject_topic"),
    NOTAS("Promedio de notas", "grade_avg"),
    ASISTENCIA("Asistencia a clase", "attendance_avg"),
    ESTUDIO("Tiempo de estudio", "intensity_hours"),
    METODO("Método de estudio", "study_method"),
    INTERES("Interés en la asignatura", "interest_level"),
    DIFICULTAD("Dificultad percibida", "difficulty_perception")
}

/** Qué puede hacer el estudiante para completar un dato. Sin acción = solo hay que esperar (p. ej. más clases). */
enum class AccionDato(val texto: String) {
    DATOS_ASIGNATURA("Completar"),
    DIFICULTAD("Responder"),
    ENFOQUE("Estudiar ahora"),
    REGISTRAR_ESTUDIO("Registrar estudio"),
    CALIFICACIONES("Registrar nota")
}

data class EstadoDato(
    val parametro: ParametroModelo,
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
 * Nunca rellena un dato que falta: si algo no está, el resultado lo dice y no se construye la petición.
 *
 * Con [tarea] = null evalúa solo la asignatura (panel de progreso): el tema se elige en cada tarea.
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

        // ÁREA
        val area = asignatura.areaModelo?.takeIf { CatalogoModelo.esArea(it) }
        estados += if (area != null) {
            EstadoDato(ParametroModelo.AREA, true, CatalogoModelo.etiqueta(area), "Confirmada por ti")
        } else {
            val sugerida = CatalogoModelo.sugerirArea(asignatura.nombre)
            EstadoDato(
                ParametroModelo.AREA, false, "",
                if (sugerida != null) "Confirma el área (sugerida: ${CatalogoModelo.etiqueta(sugerida)})" else "Elige a qué área pertenece",
                AccionDato.DATOS_ASIGNATURA
            )
        }

        // TEMA (solo al predecir una tarea): no se le pregunta al estudiante, sale del título de la tarea
        val tema = if (tarea != null && area != null) temaDe(tarea, area) else null
        if (tarea != null) {
            estados += when {
                area == null -> EstadoDato(ParametroModelo.TEMA, false, "", "Primero indica el área de la asignatura", AccionDato.DATOS_ASIGNATURA)
                tema == null -> EstadoDato(ParametroModelo.TEMA, false, "", "La tarea no tiene título")
                CatalogoModelo.esTema(area, tema) -> EstadoDato(ParametroModelo.TEMA, true, CatalogoModelo.etiqueta(tema), "Tomado de la tarea")
                else -> {
                    avisos += "El modelo no tiene «$tema» entre sus temas de ${CatalogoModelo.etiqueta(area)}: predice con el resto de tus datos"
                    EstadoDato(ParametroModelo.TEMA, true, tema, "Título de la tarea")
                }
            }
        }

        // NOTAS
        val medidaNotas = ReglasExtraccion.notas(notas.map { nota ->
            nota.valor to ReglasExtraccion.escalaDe(nota.tipo, asignatura.escalaMin, asignatura.escalaMax, asignatura.tipoCalificacion)
        })
        estados += when {
            medidaNotas.usadas >= ReglasExtraccion.MIN_NOTAS -> EstadoDato(
                ParametroModelo.NOTAS, true, "${decimal(medidaNotas.promedio10!!)} / 10",
                "${medidaNotas.usadas} notas" + if (medidaNotas.fueraDeEscala > 0) " · ${medidaNotas.fueraDeEscala} fuera de la escala, no se usan" else ""
            )
            medidaNotas.sinEscala > 0 -> EstadoDato(
                ParametroModelo.NOTAS, false, "", "Indica la escala de notas de la asignatura para usar tus ${medidaNotas.sinEscala + medidaNotas.usadas} notas",
                AccionDato.DATOS_ASIGNATURA
            )
            else -> EstadoDato(
                ParametroModelo.NOTAS, false, "", "${medidaNotas.usadas} de ${ReglasExtraccion.MIN_NOTAS} notas registradas", AccionDato.CALIFICACIONES
            )
        }

        // ASISTENCIA
        val medidaAsistencia = ReglasExtraccion.asistencia(asistencias, ahora.toLocalDate())
        estados += if (medidaAsistencia.respondidas >= ReglasExtraccion.MIN_CLASES) {
            val reciente = medidaAsistencia.porcentajeReciente?.let { " · últimas 4 semanas: ${porcentaje(it)}" }.orEmpty()
            EstadoDato(
                ParametroModelo.ASISTENCIA, true, porcentaje(medidaAsistencia.porcentaje),
                "${medidaAsistencia.asistidas} de ${medidaAsistencia.respondidas} clases$reciente"
            )
        } else if (!tieneClases(asignatura)) {
            EstadoDato(ParametroModelo.ASISTENCIA, false, "", "La asignatura no tiene días y hora de clase: no se puede preguntar la asistencia")
        } else {
            EstadoDato(
                ParametroModelo.ASISTENCIA, false, "",
                "${medidaAsistencia.respondidas} de ${ReglasExtraccion.MIN_CLASES} clases respondidas. Se pregunta al terminar cada clase"
            )
        }

        // TIEMPO DE ESTUDIO
        val medidaEstudio = ReglasExtraccion.intensidad(sesiones, ahoraMillis, ahora.zone)
        estados += when {
            medidaEstudio == null -> EstadoDato(
                ParametroModelo.ESTUDIO, false, "", "Aún no hay sesiones de estudio de esta asignatura", AccionDato.ENFOQUE
            )
            medidaEstudio.diasDeHistorial < ReglasExtraccion.MIN_DIAS_HISTORIAL_ESTUDIO -> EstadoDato(
                ParametroModelo.ESTUDIO, false, "${decimal(medidaEstudio.horasPorSemana)} h/semana",
                "Llevas ${medidaEstudio.diasDeHistorial} de ${ReglasExtraccion.MIN_DIAS_HISTORIAL_ESTUDIO} días de registro", AccionDato.REGISTRAR_ESTUDIO
            )
            else -> EstadoDato(
                ParametroModelo.ESTUDIO, true, "${decimal(medidaEstudio.horasPorSemana)} h/semana",
                "Promedio de las últimas ${minOf(ReglasExtraccion.VENTANA_ESTUDIO_DIAS, medidaEstudio.diasDeHistorial) / 7} semanas"
            )
        }
        if (medidaEstudio != null && medidaEstudio.diasDeHistorial >= ReglasExtraccion.MIN_DIAS_HISTORIAL_ESTUDIO &&
            medidaEstudio.horasPorSemana !in CatalogoModelo.RANGO_INTENSIDAD
        ) {
            avisos += "Tu tiempo de estudio (${decimal(medidaEstudio.horasPorSemana)} h/semana) está fuera del rango con que se entrenó el modelo (1 a 6 h)"
        }

        // MÉTODO
        val metodo = ReglasExtraccion.metodo(sesiones, suyas)
        estados += if (metodo != null) {
            EstadoDato(ParametroModelo.METODO, true, CatalogoModelo.etiqueta(metodo), "Según tus últimas sesiones y respuestas")
        } else {
            EstadoDato(ParametroModelo.METODO, false, "", "Cuéntanos cómo estudias esta asignatura", AccionDato.DATOS_ASIGNATURA)
        }

        // INTERÉS
        val interes = ReglasExtraccion.interes(suyas)
        estados += when {
            interes == null -> EstadoDato(ParametroModelo.INTERES, false, "", "Indica cuánto te interesa la asignatura", AccionDato.DATOS_ASIGNATURA)
            !ReglasExtraccion.interesVigente(interes, ahoraMillis) -> EstadoDato(
                ParametroModelo.INTERES, false, "${decimal(interes.valor!!.toDouble())} / 10",
                "Tu respuesta tiene más de ${ReglasExtraccion.VIGENCIA_INTERES_DIAS} días: actualízala", AccionDato.DATOS_ASIGNATURA
            )
            else -> EstadoDato(ParametroModelo.INTERES, true, "${decimal(interes.valor!!.toDouble())} / 10", "Tu última respuesta")
        }

        // DIFICULTAD
        val dificultad = ReglasExtraccion.dificultad(suyas, tarea?.tema)
        estados += if (dificultad != null) {
            EstadoDato(
                ParametroModelo.DIFICULTAD, true, "${decimal(dificultad.first)} / 10",
                if (dificultad.second == 1) "1 respuesta" else "Media de ${dificultad.second} respuestas"
            )
        } else {
            EstadoDato(ParametroModelo.DIFICULTAD, false, "", "Se pregunta al entregar una tarea; también puedes responder ahora", AccionDato.DIFICULTAD)
        }

        val completo = estados.all { it.listo }
        val datos = if (completo && tarea != null) {
            DatosPrediccion(
                attendanceAvg = redondear(ReglasExtraccion.asistenciaModelo(medidaAsistencia)),
                intensityHours = redondear(medidaEstudio!!.horasPorSemana),
                gradeAvg = redondear(medidaNotas.promedio10!!),
                difficultyPerception = redondear(dificultad!!.first),
                interestLevel = redondear(interes!!.valor!!.toDouble()),
                subjectName = area!!,
                subjectTopic = tema!!,
                studyMethod = metodo!!
            )
        } else null
        if (datos != null && datos.attendanceAvg !in CatalogoModelo.RANGO_ASISTENCIA) {
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

    /** Sesiones enlazadas por id, o las antiguas que solo guardaron el nombre. */
    private fun SesionEnfoqueEntity.esDe(materia: Asignatura) =
        asignaturaId == materia.id || (asignaturaId == null && this.asignatura.trim() == materia.nombre.trim())

    private fun tieneClases(asignatura: Asignatura) =
        asignatura.horario.any { it } && asignatura.duracion > 0 && AsistenciaReglas.parsearHora(asignatura.hora) != null

    private fun redondear(valor: Double) = (valor * 100).roundToInt() / 100.0

    private fun decimal(valor: Double) = String.format(Locale("es", "CO"), "%.1f", valor)

    private fun porcentaje(valor: Double) = "${valor.roundToInt()} %"

    private companion object {
        /** La API rechaza (422) textos de más de 100 caracteres. */
        const val MAX_TEXTO_API = 100
    }
}
