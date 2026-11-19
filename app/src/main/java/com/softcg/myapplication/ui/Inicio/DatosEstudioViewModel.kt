package com.softcg.myapplication.ui.Inicio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.softcg.myapplication.data.Repositories.PercepcionesRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ExtraerDatosPrediccionUseCase
import com.softcg.myapplication.domain.prediccion.GuardarDatosAsignaturaUseCase
import com.softcg.myapplication.domain.prediccion.GuardarDificultadUseCase
import com.softcg.myapplication.domain.prediccion.GuardarPercepcionesUseCase
import com.softcg.myapplication.domain.prediccion.RegistrarEstudioManualUseCase
import com.softcg.myapplication.domain.prediccion.ReglasExtraccion
import com.softcg.myapplication.domain.prediccion.ResultadoGuardarDatos
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

/** Respuestas que el estudiante ya dio de una asignatura; null las que nunca respondió. */
data class RespuestasDeclaradas(val interes: Float?, val dificultad: Float?, val metodo: String?, val horasSemana: Float?)

/**
 * Datos de estudio que alimentan las predicciones, compartido por las pantallas que los recogen
 * (asignaturas, tareas, enfoque, predicción). [cambios] avisa a todas cuando se guarda algo.
 */
@HiltViewModel
class DatosEstudioViewModel @Inject constructor(
    private val extraer: ExtraerDatosPrediccionUseCase,
    private val guardarDatosAsignatura: GuardarDatosAsignaturaUseCase,
    private val guardarDificultad: GuardarDificultadUseCase,
    private val guardarPercepciones: GuardarPercepcionesUseCase,
    private val registrarEstudio: RegistrarEstudioManualUseCase,
    private val percepciones: PercepcionesRepository,
    private val getAsignaturas: getAsignaturasUseCase
) : ViewModel() {

    private val _cambios = MutableLiveData(0)
    val cambios: LiveData<Int>
        get() = _cambios

    private fun avisarCambio(resultado: ResultadoGuardarDatos): ResultadoGuardarDatos {
        if (resultado is ResultadoGuardarDatos.Guardado) _cambios.value = (_cambios.value ?: 0) + 1
        return resultado
    }

    fun asignaturas(): List<Asignatura> = getAsignaturas()

    suspend fun progresoAsignatura(asignaturaId: Int): Extraccion = extraer.deAsignatura(asignaturaId)

    suspend fun metodoHabitual(asignaturaId: Int): String? = extraer.metodoHabitual(asignaturaId)

    /** Lo último que el estudiante declaró, para mostrarlo marcado al volver a abrir una hoja. */
    suspend fun respuestasActuales(asignaturaId: Int): RespuestasDeclaradas {
        val suyas = percepciones.deAsignatura(asignaturaId)
        fun ultima(tipo: String) = suyas.filter { it.tipo == tipo }.maxByOrNull { it.fechaMillis }
        return RespuestasDeclaradas(
            interes = ReglasExtraccion.interes(suyas)?.valor,
            dificultad = ultima(ReglasExtraccion.TIPO_DIFICULTAD)?.valor,
            metodo = ultima(ReglasExtraccion.TIPO_METODO)?.texto,
            horasSemana = ReglasExtraccion.horasDeclaradas(suyas)?.valor
        )
    }

    /** Área y escala de notas. */
    suspend fun guardarAsignatura(asignaturaId: Int, area: String?, escalaMin: Float?, escalaMax: Float?) =
        avisarCambio(guardarDatosAsignatura(asignaturaId, area, escalaMin, escalaMax, null, null))

    /** Solo el área: conserva la escala de notas que ya tuviera la asignatura. */
    suspend fun guardarArea(asignaturaId: Int, area: String): ResultadoGuardarDatos {
        val asignatura = asignaturas().firstOrNull { it.id == asignaturaId }
            ?: return ResultadoGuardarDatos.Invalido("La asignatura ya no existe")
        return guardarAsignatura(asignaturaId, area, asignatura.escalaMin, asignatura.escalaMax)
    }

    suspend fun guardarEstudio(asignaturaId: Int, horasSemana: Float?, metodo: String?) =
        avisarCambio(guardarPercepciones.estudio(asignaturaId, horasSemana, metodo))

    suspend fun guardarInteresYDificultad(asignaturaId: Int, tema: String?, interes: Float?, dificultad: Float?) =
        avisarCambio(guardarPercepciones.interesYDificultad(asignaturaId, tema, interes, dificultad))

    suspend fun dificultadDeTarea(tarea: Tarea, valor: Float) = avisarCambio(guardarDificultad.deTarea(tarea, valor))

    /** Solo se pregunta la dificultad una vez por tarea. */
    suspend fun debePreguntarDificultad(tarea: Tarea): Boolean {
        val id = tarea.id ?: return false
        return !guardarDificultad.yaRespondida(id)
    }

    suspend fun registrarEstudioManual(asignaturaId: Int, fecha: LocalDate, minutos: Int, metodo: String?) =
        avisarCambio(registrarEstudio(asignaturaId, fecha, minutos, metodo))
}
