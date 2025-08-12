package com.softcg.myapplication.ui.Inicio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.getCalificacionesUseCase
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import com.softcg.myapplication.ui.Inicio.Models.Evento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val getTareasUseCase: getTareasUseCase,
    private val getEventosUseCase: getEventosUseCase,
    private val getAsignaturasUseCase: getAsignaturasUseCase,
    private val getCalificacionesUseCase: getCalificacionesUseCase
) : ViewModel(){

    @Inject lateinit var tareasRepository: TareasRepository
    @Inject lateinit var eventosRepository: EventosRepository
    @Inject lateinit var calificacionesRespository: CalificacionesRespository


    val _asignaturas = MutableLiveData<List<String>>()
    val _tareas = MutableLiveData<List<Tarea>>()
    val _eventos = MutableLiveData<List<Evento>>()
    val _calificaciones=MutableLiveData<List<Calificacion>>()

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

    fun onAgregarTareaSelected(titulo:String,descrip:String,asignatura:String,fecha:String,prioridad:Int){
        val tarea: Tarea = Tarea(null,titulo, descrip, asignatura,fecha,prioridad)
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
    fun onAgregarCalificacionSelected(tipo:String,valor:Float,asignatura:String,descripcion:String){
        val calificacion = Calificacion(null,tipo,valor,asignatura, descripcion)
        saveCalificacion(calificacion)
        obtenerCalificaciones()
    }

    fun obtenerCalificaciones(){
        viewModelScope.launch {
            _calificaciones.value = getCalificacionesUseCase()
        }
    }
    fun saveCalificacion(calificacion: Calificacion){
        viewModelScope.launch {
            calificacionesRespository.insertCalificacion(calificacion)
        }
    }

    fun saveTarea(tarea: Tarea){
        viewModelScope.launch {
            tareasRepository.insertTarea(tarea)
        }
    }

    fun onAgregarEventoSelected(titulo:String,descrip:String, fecha:String,prioridad:Int,){
        val evento: Evento = Evento(null,titulo, descrip,fecha,prioridad)
        saveEvento(evento)
        obtenerEventos()
    }
    fun saveEvento(evento: Evento){
        viewModelScope.launch {
            eventosRepository.insertEvento(evento)
        }
    }
}