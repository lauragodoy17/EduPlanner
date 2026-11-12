package com.softcg.myapplication.domain.prediccion

import com.softcg.myapplication.data.network.PrediccionService
import com.softcg.myapplication.data.network.RespuestaApi
import org.json.JSONObject
import javax.inject.Inject

/** Pide a la API el estado de práctica, los métodos de estudio recomendados y el perfil. */
class PredecirTareaUseCase @Inject constructor(private val servicio: PrediccionService) {

    suspend operator fun invoke(datos: DatosPrediccion, topN: Int = TOP_N): RespuestaApi =
        servicio.predecir(datos.aJson(), topN)

    companion object {
        const val TOP_N = 3
    }
}

/**
 * Registro que espera POST /predict. Los nombres JSON son los del modelo; las categorías van
 * sin tildes, como en el entrenamiento ("Matematicas", "Algebra lineal"): con tilde la API no falla,
 * pero las ignora y la predicción empeora.
 */
data class DatosPrediccion(
    val attendanceAvg: Double,
    val intensityHours: Double,
    val gradeAvg: Double,
    val difficultyPerception: Double,
    val interestLevel: Double,
    val subjectName: String,
    val subjectTopic: String,
    val studyMethod: String
) {
    fun aJson(): String = JSONObject()
        .put("attendance_avg", attendanceAvg)
        .put("intensity_hours", intensityHours)
        .put("grade_avg", gradeAvg)
        .put("difficulty_perception", difficultyPerception)
        .put("interest_level", interestLevel)
        .put("subject_name", subjectName)
        .put("subject_topic", subjectTopic)
        .put("study_method", studyMethod)
        .toString()

    companion object {
        /** Datos fijos mientras la tarea no tenga sus propios campos para el modelo. */
        val PRUEBA = DatosPrediccion(
            attendanceAvg = 6.5,
            intensityHours = 4.0,
            gradeAvg = 7.0,
            difficultyPerception = 5.0,
            interestLevel = 8.0,
            subjectName = "Matematicas",
            subjectTopic = "Algebra lineal",
            studyMethod = "Pomodoro"
        )
    }
}
