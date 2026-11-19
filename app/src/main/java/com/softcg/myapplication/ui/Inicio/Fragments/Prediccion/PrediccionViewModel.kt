package com.softcg.myapplication.ui.Inicio.Fragments.Prediccion

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.EditarTareaUseCase
import com.softcg.myapplication.domain.ResultadoEdicion
import com.softcg.myapplication.data.network.RespuestaApi
import com.softcg.myapplication.domain.ia.ContextoPrediccion
import com.softcg.myapplication.domain.ia.RecomendacionTarea
import com.softcg.myapplication.domain.ia.RecomendarTareaUseCase
import com.softcg.myapplication.domain.ia.ResultadoRecomendacion
import com.softcg.myapplication.domain.prediccion.DatosPrediccion
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ExtraerDatosPrediccionUseCase
import com.softcg.myapplication.domain.prediccion.PredecirTareaUseCase
import com.softcg.myapplication.domain.prediccion.ResultadoExtraccion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Predicción de una tarea con los datos reales del estudiante: primero se extraen y validan;
 * solo si están todos se construye la petición. Nunca se envían datos inventados o de prueba.
 * El resultado del modelo se pasa a la IA (Gemini), que redacta el plan de estudio: consejo, pasos,
 * tiempo estimado y fuentes comprobadas, sin poder cambiar que eligió el modelo.
 */
@HiltViewModel
class PrediccionViewModel @Inject constructor(
    private val tareas: TareasRepository,
    private val extraer: ExtraerDatosPrediccionUseCase,
    private val predecir: PredecirTareaUseCase,
    private val asignaturas: AsignaturasRepository,
    private val editarTarea: EditarTareaUseCase,
    private val recomendar: RecomendarTareaUseCase
) : ViewModel() {

    val topN: Int = PredecirTareaUseCase.TOP_N

    /** La tarea tal como está guardada ahora (puede haberse entregado o calificado desde otra pantalla). */
    private val _tarea = MutableLiveData<Tarea?>()
    val tarea: LiveData<Tarea?>
        get() = _tarea

    private val _datos = MutableLiveData<EstadoDatos>()
    val datos: LiveData<EstadoDatos>
        get() = _datos

    private val _estado = MutableLiveData<EstadoPrediccion?>()
    val estado: LiveData<EstadoPrediccion?>
        get() = _estado

    private val _recomendacion = MutableLiveData<EstadoRecomendacion?>()
    val recomendacion: LiveData<EstadoRecomendacion?>
        get() = _recomendacion

    /** Contexto de la última predicción (con su resultado o por qué no lo hubo): lo que recibe la IA. */
    private var contexto: ContextoPrediccion? = null

    /** Aviso breve tras editar. */
    private val _mensaje = MutableLiveData<Event<String>>()
    val mensaje: LiveData<Event<String>>
        get() = _mensaje

    /** La tarea se borró: la pantalla se cierra con este aviso. */
    private val _borrada = MutableLiveData<Event<String>>()
    val borrada: LiveData<Event<String>>
        get() = _borrada

    private var tareaId: Int = -1

    /** Lo último que se envió: si los datos no cambian, no se repite la petición. */
    var datosEnviados: DatosPrediccion? = null
        private set

    /** Vuelve a leer los datos (al abrir y cada vez que el estudiante completa alguno). */
    fun cargar(tareaId: Int) {
        this.tareaId = tareaId
        viewModelScope.launch {
            val tarea = tareas.getTarea(tareaId)
            _tarea.value = tarea
            val nuevo = when (val extraccion = tarea?.let { extraer.deTarea(it) }) {
                null -> EstadoDatos.SinTarea
                Extraccion.SinSesion -> EstadoDatos.SinSesion
                Extraccion.SinAsignatura -> EstadoDatos.SinAsignatura
                is Extraccion.Lista -> EstadoDatos.Listos(extraccion.resultado)
            }
            _datos.value = nuevo
            // Nunca se genera nada al abrir (gastaría créditos de la IA sin querer): solo se muestra la recomendación
            // que el estudiante ya pidió para esta tarea, si sigue en memoria
            if (_recomendacion.value == null) {
                tarea?.let(recomendar::enCache)?.let { _recomendacion.value = EstadoRecomendacion.Lista(it) }
            }
        }
    }

    fun nombresAsignaturas(): List<String> = asignaturas.getAllAsignaturas().map { it.nombre }

    /** Guarda lo editado y vuelve a extraer los datos (la asignatura o el título pueden haber cambiado). */
    fun editar(tarea: Tarea) {
        viewModelScope.launch {
            _mensaje.value = Event(
                when (editarTarea(tarea.copy(id = tareaId))) {
                    ResultadoEdicion.Guardada -> "Tarea actualizada"
                    ResultadoEdicion.NombreVacio -> "Escribe el nombre de la tarea"
                    else -> "No se pudo guardar: la tarea ya no existe"
                }
            )
            cargar(tareaId)
        }
    }

    fun borrar() {
        val tarea = _tarea.value ?: return
        viewModelScope.launch {
            tareas.deleteTarea(tarea)
            _borrada.value = Event("Tarea eliminada")
        }
    }

    /**
     * Botón "Generar recomendaciones": es lo único que llama a la API y a la IA. Con datos nuevos, o si la predicción
     * anterior falló, vuelve a predecir (y a recomendar). Con los mismos datos y una predicción buena no repite la API:
     * pide a la IA otro plan (o reintenta si falló). Si ya hay un plan a la vista, siempre se pide uno distinto.
     */
    fun predecirDeNuevo() {
        val listos = (_datos.value as? EstadoDatos.Listos)?.resultado?.datos ?: return
        if (ocupado()) return
        val anterior = _estado.value
        val prediccionBuena = anterior is EstadoPrediccion.Respuesta && anterior.exitosa
        val previo = contexto
        if (listos == datosEnviados && prediccionBuena && previo != null) {
            recomendarCon(previo, nueva = _recomendacion.value is EstadoRecomendacion.Lista)
        } else {
            enviar(listos)
        }
    }

    private fun ocupado() =
        _estado.value == EstadoPrediccion.Cargando || _recomendacion.value == EstadoRecomendacion.Generando

    private fun enviar(datos: DatosPrediccion) {
        if (ocupado()) return
        datosEnviados = datos
        val hayPlanAnterior = _recomendacion.value is EstadoRecomendacion.Lista
        _estado.value = EstadoPrediccion.Cargando
        _recomendacion.value = EstadoRecomendacion.Generando
        viewModelScope.launch {
            val respuesta = predecir(datos, topN)
            _estado.value = when (respuesta) {
                is RespuestaApi.Http -> EstadoPrediccion.Respuesta(respuesta.codigo, respuesta.cuerpo, respuesta.exitosa)
                RespuestaApi.SinSesion -> EstadoPrediccion.Error("No hay una sesión de Firebase iniciada")
                is RespuestaApi.ErrorRed -> EstadoPrediccion.Error("No se pudo conectar con la API: ${respuesta.mensaje}")
            }
            // Sin sesión tampoco hay IA (Firebase AI Logic la necesita); en los demás casos la IA recomienda igual,
            // aclarando que el modelo no respondió
            if (respuesta == RespuestaApi.SinSesion) {
                _recomendacion.value = null
                return@launch
            }
            val nuevo = recomendar.contextoDeRespuesta(datos, respuesta)
            contexto = nuevo
            generarRecomendacion(nuevo, nueva = hayPlanAnterior)
        }
    }

    private fun recomendarCon(contexto: ContextoPrediccion, nueva: Boolean) {
        _recomendacion.value = EstadoRecomendacion.Generando
        viewModelScope.launch { generarRecomendacion(contexto, nueva) }
    }

    private suspend fun generarRecomendacion(contexto: ContextoPrediccion, nueva: Boolean) {
        val tarea = _tarea.value ?: run {
            _recomendacion.value = null
            return
        }
        _recomendacion.value = when (val resultado = recomendar.conContexto(tarea, contexto, nueva)) {
            is ResultadoRecomendacion.Exito -> EstadoRecomendacion.Lista(resultado.recomendacion)
            ResultadoRecomendacion.SinConexion ->
                EstadoRecomendacion.Error("No se pudo conectar con la IA. Revisa tu conexión a internet e inténtalo de nuevo.")
            ResultadoRecomendacion.Error ->
                EstadoRecomendacion.Error("No se pudieron generar las recomendaciones. Inténtalo de nuevo en unos minutos.")
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

sealed class EstadoRecomendacion {
    object Generando : EstadoRecomendacion()
    data class Lista(val recomendacion: RecomendacionTarea) : EstadoRecomendacion()
    data class Error(val mensaje: String) : EstadoRecomendacion()
}
