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
import com.softcg.myapplication.domain.prediccion.RegistrarEstudioManualUseCase
import com.softcg.myapplication.domain.prediccion.ReglasExtraccion
import com.softcg.myapplication.domain.prediccion.ResultadoGuardarDatos
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

/**
 * Datos de estudio que alimentan las predicciones, compartido por las pantallas que los recogen
 * (asignaturas, tareas, enfoque, predicción). [cambios] avisa a todas cuando se guarda algo.
 */
@HiltViewModel
class DatosEstudioViewModel @Inject constructor(
    private val extraer: ExtraerDatosPrediccionUseCase,
    private val guardarDatosAsignatura: GuardarDatosAsignaturaUseCase,
    private val guardarDificultad: GuardarDificultadUseCase,
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

    suspend fun progresoTarea(tarea: Tarea): Extraccion = extraer.deTarea(tarea)

    suspend fun metodoHabitual(asignaturaId: Int): String? = extraer.metodoHabitual(asignaturaId)

    /** Interés y método que el estudiante ya indicó, para mostrarlos marcados al volver a abrir la hoja. */
    suspend fun respuestasActuales(asignaturaId: Int): Pair<Float?, String?> {
        val suyas = percepciones.deAsignatura(asignaturaId)
        val interes = ReglasExtraccion.interes(suyas)?.valor
        val metodo = suyas.firstOrNull { it.tipo == ReglasExtraccion.TIPO_METODO }?.texto
        return interes to metodo
    }

    suspend fun guardarAsignatura(
        asignaturaId: Int, area: String?, escalaMin: Float?, escalaMax: Float?, interes: Float?, metodo: String?
    ) = avisarCambio(guardarDatosAsignatura(asignaturaId, area, escalaMin, escalaMax, interes, metodo))

    suspend fun dificultadDeTarea(tarea: Tarea, valor: Float) = avisarCambio(guardarDificultad.deTarea(tarea, valor))

    suspend fun dificultadDeAsignatura(asignaturaId: Int, tema: String?, valor: Float) =
        avisarCambio(guardarDificultad.deAsignatura(asignaturaId, tema, valor))

    /** Solo se pregunta la dificultad una vez por tarea. */
    suspend fun debePreguntarDificultad(tarea: Tarea): Boolean {
        val id = tarea.id ?: return false
        return !guardarDificultad.yaRespondida(id)
    }

    suspend fun registrarEstudioManual(asignaturaId: Int, fecha: LocalDate, minutos: Int, metodo: String?) =
        avisarCambio(registrarEstudio(asignaturaId, fecha, minutos, metodo))
}
