package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.domain.CrearAsignaturaUseCase
import com.softcg.myapplication.domain.NuevaAsignatura
import com.softcg.myapplication.domain.ResultadoCrearAsignatura
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.prediccion.Extraccion
import com.softcg.myapplication.domain.prediccion.ExtraerDatosPrediccionUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Cuántos datos para predicciones tiene una asignatura, para su tarjeta. */
data class ResumenDatos(val listos: Int, val total: Int, val actualizarInteres: Boolean)

@HiltViewModel
class AsignaturasViewModel @Inject constructor(
    private val getAsignaturasUseCase: getAsignaturasUseCase,
    private val crearAsignatura: CrearAsignaturaUseCase,
    private val extraerDatos: ExtraerDatosPrediccionUseCase
) : ViewModel(){

    companion object {
        /** Cada mes se recuerda confirmar el interés: puede cambiar durante el periodo. */
        const val DIAS_RECORDAR_INTERES = 30L
    }

    @Inject lateinit var asignaturasRepository : AsignaturasRepository

    val _asignaturas= MutableLiveData<List<Asignatura>>()

    /** EduCoins ganados al registrar una clase; solo se emite si el movimiento se guardó. */
    private val _recompensa = MutableLiveData<Event<Int>>()
    val recompensa: LiveData<Event<Int>>
        get() = _recompensa

    /** Id de la asignatura recién creada: se le piden sus datos para predicciones. */
    private val _asignaturaCreada = MutableLiveData<Event<Int>>()
    val asignaturaCreada: LiveData<Event<Int>>
        get() = _asignaturaCreada

    private val _resumenDatos = MutableLiveData<Map<Int, ResumenDatos>>(emptyMap())
    val resumenDatos: LiveData<Map<Int, ResumenDatos>>
        get() = _resumenDatos

    fun obtenerAsignaturas (){
        viewModelScope.launch {
            val asignaturas = getAsignaturasUseCase()
            _asignaturas.value = asignaturas
            actualizarResumenDatos(asignaturas)
        }
    }

    fun actualizarResumenDatos(asignaturas: List<Asignatura> = _asignaturas.value.orEmpty()) {
        viewModelScope.launch {
            _resumenDatos.value = asignaturas.mapNotNull { asignatura ->
                val id = asignatura.id ?: return@mapNotNull null
                val resultado = (extraerDatos.deAsignatura(id) as? Extraccion.Lista)?.resultado ?: return@mapNotNull null
                val interesViejo = (resultado.diasDesdeInteres ?: 0) >= DIAS_RECORDAR_INTERES
                id to ResumenDatos(resultado.listos, resultado.total, interesViejo)
            }.toMap()
        }
    }
    fun deleteAsignatura(asignatura: Asignatura){
        viewModelScope.launch {
            asignaturasRepository.deleteAsignatura(asignatura)
            obtenerAsignaturas()
        }
    }

    fun onAgregarAsignaturaSelected(nueva: NuevaAsignatura) {
        viewModelScope.launch {
            val resultado = crearAsignatura(nueva)
            obtenerAsignaturas()
            if (resultado is ResultadoCrearAsignatura.Creada) {
                _asignaturaCreada.value = Event(resultado.id)
                resultado.recompensa?.let { _recompensa.value = Event(it) }
            }
        }
    }
}