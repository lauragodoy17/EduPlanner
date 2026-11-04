package com.softcg.myapplication.ui.Inicio.Fragments.Perfil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.domain.gamificacion.AbandonarEnfoqueUseCase
import com.softcg.myapplication.domain.gamificacion.CompletarEnfoqueUseCase
import com.softcg.myapplication.domain.gamificacion.IniciarEnfoqueUseCase
import com.softcg.myapplication.domain.gamificacion.ObtenerEnfoqueActivoUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoCompletarEnfoque
import com.softcg.myapplication.domain.gamificacion.ResultadoIniciarEnfoque
import com.softcg.myapplication.domain.gamificacion.SesionEnfoque
import com.softcg.myapplication.domain.getAsignaturasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnfoqueViewModel @Inject constructor(
    private val obtenerActiva: ObtenerEnfoqueActivoUseCase,
    private val iniciarEnfoque: IniciarEnfoqueUseCase,
    private val completarEnfoque: CompletarEnfoqueUseCase,
    private val abandonarEnfoque: AbandonarEnfoqueUseCase,
    private val getAsignaturas: getAsignaturasUseCase,
    private val reloj: Reloj
) : ViewModel() {

    /** La sesión en curso, leída de la base: sigue ahí aunque se cierre la app. */
    private val _sesion = MutableLiveData<SesionEnfoque?>()
    val sesion: LiveData<SesionEnfoque?>
        get() = _sesion

    private val _resultadoInicio = MutableLiveData<Event<ResultadoIniciarEnfoque>>()
    val resultadoInicio: LiveData<Event<ResultadoIniciarEnfoque>>
        get() = _resultadoInicio

    private val _resultadoFin = MutableLiveData<Event<ResultadoCompletarEnfoque>>()
    val resultadoFin: LiveData<Event<ResultadoCompletarEnfoque>>
        get() = _resultadoFin

    private var completando = false

    val asignaturas: List<String> get() = getAsignaturas().map { it.nombre }

    fun ahoraMillis(): Long = reloj.ahora().toInstant().toEpochMilli()

    fun cargar() {
        viewModelScope.launch {
            _sesion.value = obtenerActiva()
        }
    }

    fun iniciar(minutos: Int, asignatura: String) {
        viewModelScope.launch {
            val resultado = iniciarEnfoque(minutos, asignatura)
            if (resultado is ResultadoIniciarEnfoque.Iniciada) _sesion.value = resultado.sesion
            _resultadoInicio.value = Event(resultado)
        }
    }

    /** Se puede llamar varias veces por tic: solo se intenta una a la vez. */
    fun completar() {
        if (completando) return
        completando = true
        viewModelScope.launch {
            val resultado = completarEnfoque()
            if (resultado !is ResultadoCompletarEnfoque.AunNoTermina) _sesion.value = null
            _resultadoFin.value = Event(resultado)
            completando = false
        }
    }

    fun abandonar() {
        viewModelScope.launch {
            if (abandonarEnfoque()) _sesion.value = null
        }
    }
}
