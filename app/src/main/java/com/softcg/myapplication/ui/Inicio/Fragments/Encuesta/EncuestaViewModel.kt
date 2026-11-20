package com.softcg.myapplication.ui.Inicio.Fragments.Encuesta

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.domain.encuesta.AlmacenEncuesta
import com.softcg.myapplication.domain.encuesta.EncuestaCatalogo
import com.softcg.myapplication.domain.encuesta.EnviarEncuestaUseCase
import com.softcg.myapplication.domain.encuesta.ReglasEncuesta
import com.softcg.myapplication.domain.encuesta.RespuestasEncuesta
import com.softcg.myapplication.domain.encuesta.ResultadoEnvioEncuesta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstadoEncuesta {
    object Respondiendo : EstadoEncuesta()
    object Enviando : EstadoEncuesta()
    object Enviada : EstadoEncuesta()
}

/** Una sección por página; cada respuesta se guarda al momento en el borrador del dispositivo. */
@HiltViewModel
class EncuestaViewModel @Inject constructor(
    private val almacen: AlmacenEncuesta,
    private val enviarEncuesta: EnviarEncuestaUseCase
) : ViewModel() {

    private val respuestas = almacen.borrador().toMutableMap()

    private val _seccion = MutableLiveData(almacen.seccion().coerceIn(0, EncuestaCatalogo.secciones.lastIndex))
    /** Índice de la sección que se muestra. */
    val seccion: LiveData<Int> get() = _seccion

    private val _estado = MutableLiveData<EstadoEncuesta>(if (almacen.enviada()) EstadoEncuesta.Enviada else EstadoEncuesta.Respondiendo)
    val estado: LiveData<EstadoEncuesta> get() = _estado

    /** Ids de las preguntas sin responder de la sección actual, al intentar avanzar. */
    private val _faltantes = MutableLiveData<Event<List<String>>>()
    val faltantes: LiveData<Event<List<String>>> get() = _faltantes

    private val _mensaje = MutableLiveData<Event<String>>()
    val mensaje: LiveData<Event<String>> get() = _mensaje

    val totalSecciones: Int get() = EncuestaCatalogo.secciones.size

    fun respuestas(): RespuestasEncuesta = respuestas.toMap()

    fun respuesta(id: String): String? = respuestas[id]

    /** Vacío borra la respuesta (p. ej. un texto que se dejó en blanco). */
    fun responder(id: String, valor: String?) {
        if (valor.isNullOrEmpty()) respuestas.remove(id) else respuestas[id] = valor
        almacen.guardarBorrador(respuestas)
    }

    fun siguiente() {
        val actual = _seccion.value ?: 0
        val faltan = ReglasEncuesta.faltantes(EncuestaCatalogo.secciones[actual], respuestas)
        if (faltan.isNotEmpty()) {
            _faltantes.value = Event(faltan)
            return
        }
        if (actual == EncuestaCatalogo.secciones.lastIndex) enviar() else irA(actual + 1)
    }

    fun anterior() {
        val actual = _seccion.value ?: 0
        if (actual > 0) irA(actual - 1)
    }

    private fun irA(indice: Int) {
        _seccion.value = indice
        almacen.guardarSeccion(indice)
    }

    private fun enviar() {
        if (_estado.value == EstadoEncuesta.Enviando) return
        _estado.value = EstadoEncuesta.Enviando
        viewModelScope.launch {
            val resultado = enviarEncuesta(respuestas.toMap())
            _estado.value = if (resultado == ResultadoEnvioEncuesta.Enviada) EstadoEncuesta.Enviada else EstadoEncuesta.Respondiendo
            when (resultado) {
                ResultadoEnvioEncuesta.Enviada -> Unit
                is ResultadoEnvioEncuesta.Incompleta -> {
                    irA(resultado.seccion)
                    _faltantes.value = Event(ReglasEncuesta.faltantes(EncuestaCatalogo.secciones[resultado.seccion], respuestas))
                }
                ResultadoEnvioEncuesta.NoConfigurada -> _mensaje.value = Event("La encuesta aún no está disponible. Tus respuestas quedan guardadas")
                ResultadoEnvioEncuesta.SinConexion -> _mensaje.value = Event("Sin conexión. Tus respuestas quedan guardadas: inténtalo de nuevo")
                is ResultadoEnvioEncuesta.Error -> _mensaje.value = Event("No se pudo enviar. Tus respuestas quedan guardadas: inténtalo de nuevo")
            }
        }
    }
}
