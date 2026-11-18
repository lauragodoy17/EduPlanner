package com.softcg.myapplication.ui.Inicio.Fragments.Prediccion

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.RespuestaApi
import com.softcg.myapplication.domain.prediccion.DatosPrediccion
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ExtraerDatosPrediccionUseCase
import com.softcg.myapplication.domain.prediccion.PredecirTareaUseCase
import com.softcg.myapplication.domain.prediccion.ResultadoExtraccion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Predicción de una tarea con los datos reales del estudiante: primero se extraen y validan;
 * solo si están todos se construye la petición. Nunca se envían datos inventados o de prueba.
 */
@HiltViewModel
class PrediccionViewModel @Inject constructor(
    private val tareas: TareasRepository,
    private val extraer: ExtraerDatosPrediccionUseCase,
    private val predecir: PredecirTareaUseCase
) : ViewModel() {

    val topN: Int = PredecirTareaUseCase.TOP_N

    private val _datos = MutableLiveData<EstadoDatos>()
    val datos: LiveData<EstadoDatos>
        get() = _datos

    private val _estado = MutableLiveData<EstadoPrediccion?>()
    val estado: LiveData<EstadoPrediccion?>
        get() = _estado

    /** Lo último que se envió: si los datos no cambian, no se repite la petición. */
    var datosEnviados: DatosPrediccion? = null
        private set

    /** Vuelve a leer los datos (al abrir y cada vez que el estudiante completa alguno). */
    fun cargar(tareaId: Int) {
        viewModelScope.launch {
            val tarea = tareas.getTarea(tareaId)
            val nuevo = when (val extraccion = tarea?.let { extraer.deTarea(it) }) {
                null -> EstadoDatos.SinTarea
                Extraccion.SinSesion -> EstadoDatos.SinSesion
                Extraccion.SinAsignatura -> EstadoDatos.SinAsignatura
                is Extraccion.Lista -> EstadoDatos.Listos(extraccion.resultado)
            }
            _datos.value = nuevo
            // La primera vez que todo está completo se predice sin pedir nada más
            if (nuevo is EstadoDatos.Listos && datosEnviados == null && _estado.value == null) {
                nuevo.resultado.datos?.let { enviar(it) }
            }
        }
    }

    /** Envía los datos ya validados; no hace nada si faltan o si son los mismos que la última vez. */
    fun predecirDeNuevo() {
        val listos = (_datos.value as? EstadoDatos.Listos)?.resultado?.datos ?: return
        if (listos == datosEnviados && _estado.value !is EstadoPrediccion.Error) return
        enviar(listos)
    }

    private fun enviar(datos: DatosPrediccion) {
        if (_estado.value == EstadoPrediccion.Cargando) return
        datosEnviados = datos
        _estado.value = EstadoPrediccion.Cargando
        viewModelScope.launch {
            _estado.value = when (val respuesta = predecir(datos, topN)) {
                is RespuestaApi.Http -> EstadoPrediccion.Respuesta(respuesta.codigo, respuesta.cuerpo, respuesta.exitosa)
                RespuestaApi.SinSesion -> EstadoPrediccion.Error("No hay una sesión de Firebase iniciada")
                is RespuestaApi.ErrorRed -> EstadoPrediccion.Error("No se pudo conectar con la API: ${respuesta.mensaje}")
            }
        }
    }
}

sealed class EstadoDatos {
    data class Listos(val resultado: ResultadoExtraccion) : EstadoDatos()
    object SinTarea : EstadoDatos()
    object SinAsignatura : EstadoDatos()
    object SinSesion : EstadoDatos()
}

sealed class EstadoPrediccion {
    object Cargando : EstadoPrediccion()
    data class Respuesta(val codigo: Int, val cuerpo: String, val exitosa: Boolean) : EstadoPrediccion()
    data class Error(val mensaje: String) : EstadoPrediccion()
}
