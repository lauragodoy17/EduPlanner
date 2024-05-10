package com.softcg.myapplication.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.evento.model.Evento
import com.softcg.myapplication.ui.home.model.AgendaItem
import com.softcg.myapplication.ui.tarea.model.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModelHome @Inject constructor(
    private val getTareasUseCase: getTareasUseCase,
    private val getEventosUseCase: getEventosUseCase
    ) : ViewModel(){

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

    //FRAGMENT HOME
    @Inject lateinit var tareasRepository: TareasRepository
    @Inject lateinit var eventosRepository: EventosRepository
    val _tareas= MutableLiveData<List<Tarea>>()
    val _eventos= MutableLiveData<List<Evento>>()

    fun obtenerEventos (){
        viewModelScope.launch {
            _eventos.value = getEventosUseCase()
        }
    }

    fun obtenerTareas (){
        viewModelScope.launch {
            _tareas.value = getTareasUseCase()
        }
    }

    fun deleteTarea(tarea: Tarea){
        viewModelScope.launch {
            tareasRepository.deleteTarea(tarea)
            obtenerTareas()
            obtenerAgendaList()
        }
    }

    fun deleteEvento(evento: Evento){
        viewModelScope.launch {
            eventosRepository.deleteEvento(evento)
            obtenerEventos()
            obtenerAgendaList()
        }
    }


    //FRAGMENT AGENDA
    val _listAgenda= MutableLiveData<List<AgendaItem>>()

    fun obtenerAgendaList() {
        var list :List<AgendaItem> = emptyList()

        if (_tareas.value?.isNotEmpty() == true){
            list = _tareas.value!!.map {
                AgendaItem(id = it.id,titulo = it.titulo, descrip = it.descrip, asignatura = it.asignatura, fecha = it.fecha)
            }
        }
        if (_eventos.value?.isNotEmpty() == true){
            val dos: List<AgendaItem> = _eventos.value!!.map {
                AgendaItem(id = it.id,titulo = it.titulo, descrip = it.descrip, asignatura = null, fecha = it.fecha)
            }
            list = list.plus(dos)
        }
        _listAgenda.value=list
    }
    fun deleteTarea(agendaItem: AgendaItem){
        val tarea=Tarea(agendaItem.id,agendaItem.titulo,agendaItem.descrip,agendaItem.asignatura!!,agendaItem.fecha)
        deleteTarea(tarea)
    }

    fun deleteEvento(agendaItem: AgendaItem){
        val evento = Evento(agendaItem.id,agendaItem.titulo,agendaItem.descrip,agendaItem.fecha)
        deleteEvento(evento)
    }

}