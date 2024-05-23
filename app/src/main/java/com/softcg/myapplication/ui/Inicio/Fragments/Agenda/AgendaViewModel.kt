package com.softcg.myapplication.ui.Inicio.Fragments.Agenda

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Models.AgendaItem
import com.softcg.myapplication.ui.evento.model.Evento
import com.softcg.myapplication.ui.tarea.model.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val getTareasUseCase: getTareasUseCase,
    private val getEventosUseCase: getEventosUseCase
) : ViewModel(){

    @Inject lateinit var tareasRepository: TareasRepository
    @Inject lateinit var eventosRepository: EventosRepository
    val _tareas= MutableLiveData<List<Tarea>>()
    val _eventos= MutableLiveData<List<Evento>>()
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
        val tarea= Tarea(agendaItem.id,agendaItem.titulo,agendaItem.descrip,agendaItem.asignatura!!,agendaItem.fecha)
        deleteTarea(tarea)
    }

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

    fun deleteEvento(agendaItem: AgendaItem){
        val evento = Evento(agendaItem.id,agendaItem.titulo,agendaItem.descrip,agendaItem.fecha)
        deleteEvento(evento)
    }

}