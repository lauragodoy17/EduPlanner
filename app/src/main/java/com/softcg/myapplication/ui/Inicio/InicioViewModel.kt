package com.softcg.myapplication.ui.Inicio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import com.softcg.myapplication.ui.Inicio.Models.Evento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val getTareasUseCase: getTareasUseCase,
    private val getEventosUseCase: getEventosUseCase,
    private val getAsignaturasUseCase: getAsignaturasUseCase
) : ViewModel(){

    @Inject lateinit var tareasRepository: TareasRepository
    @Inject lateinit var eventosRepository: EventosRepository


    val _asignaturas = MutableLiveData<List<String>>()
    val _tareas = MutableLiveData<List<Tarea>>()
    val _eventos = MutableLiveData<List<Evento>>()

    //HOME ACTIVITY
    private val _navigateToTarea = MutableLiveData<Event<Boolean>>()
    val navigateToTarea: LiveData<Event<Boolean>>
        get() = _navigateToTarea

    private val _navigateToEvento= MutableLiveData<Event<Boolean>>()
    val navigateToEvento: LiveData<Event<Boolean>>
        get() = _navigateToEvento

    private val _navigateToCalificacion= MutableLiveData<Event<Boolean>>()
    val navigateToCalificacion: LiveData<Event<Boolean>>
        get() = _navigateToCalificacion

    fun onTareaSelected(){
        _navigateToTarea.value= Event(true)
    }

    fun onEventoSelected(){
        _navigateToEvento.value= Event(true)
    }
    fun onCalificacionSelected(){
        _navigateToCalificacion.value= Event(true)
    }

    fun onAgregarTareaSelected(titulo:String,descrip:String,asignatura:String,fecha:String){
        val tarea: Tarea = Tarea(null,titulo, descrip, asignatura,fecha)
        saveTarea(tarea)
        obtenerTareas()
    }

    fun obtenerTareas (){
        viewModelScope.launch {
            _tareas.value = getTareasUseCase()
        }
    }
    fun obtenerEventos (){
        viewModelScope.launch {
            _eventos.value = getEventosUseCase()
        }
    }

    fun obtenerAsignaturas(){
        viewModelScope.launch {
            _asignaturas.value = getAsignaturasUseCase().map { it.nombre }
        }
        if(_asignaturas.value?.isEmpty() == true){
            _asignaturas.value= listOf("Agregar Asignatura")
        }
    }

    fun saveTarea(tarea: Tarea){
        viewModelScope.launch {
            tareasRepository.insertTarea(tarea)
        }
    }

    fun onAgregarEventoSelected(titulo:String,descrip:String, fecha:String,){
        val evento: Evento = Evento(null,titulo, descrip,fecha)
        saveEvento(evento)
        obtenerEventos()
    }
    fun saveEvento(evento: Evento){
        viewModelScope.launch {
            eventosRepository.insertEvento(evento)
        }
    }
}