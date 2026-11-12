package com.softcg.myapplication.ui.Inicio.Fragments.Prediccion

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.network.RespuestaApi
import com.softcg.myapplication.domain.prediccion.DatosPrediccion
import com.softcg.myapplication.domain.prediccion.PredecirTareaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrediccionViewModel @Inject constructor(
    private val predecir: PredecirTareaUseCase
) : ViewModel() {

    val datosEnviados: DatosPrediccion = DatosPrediccion.PRUEBA
    val topN: Int = PredecirTareaUseCase.TOP_N

    private val _estado = MutableLiveData<EstadoPrediccion>()
    val estado: LiveData<EstadoPrediccion>
        get() = _estado

    /** Una sola petición por pantalla: al girar el teléfono no se vuelve a enviar. */
    fun consultarSiHaceFalta() {
        if (_estado.value != null) return
        _estado.value = EstadoPrediccion.Cargando
        viewModelScope.launch {
            _estado.value = when (val respuesta = predecir(datosEnviados, topN)) {
                is RespuestaApi.Http -> EstadoPrediccion.Respuesta(respuesta.codigo, respuesta.cuerpo, respuesta.exitosa)
                RespuestaApi.SinSesion -> EstadoPrediccion.Error("No hay una sesión de Firebase iniciada")
                is RespuestaApi.ErrorRed -> EstadoPrediccion.Error("No se pudo conectar con la API: ${respuesta.mensaje}")
            }
        }
    }
}

sealed class EstadoPrediccion {
    object Cargando : EstadoPrediccion()
    data class Respuesta(val codigo: Int, val cuerpo: String, val exitosa: Boolean) : EstadoPrediccion()
    data class Error(val mensaje: String) : EstadoPrediccion()
}
