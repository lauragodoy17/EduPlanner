package com.softcg.myapplication.ui.Inicio.Fragments.Agenda

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.AgendaItem
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val getTareasUseCase: getTareasUseCase,
    private val getEventosUseCase: getEventosUseCase,
    private val reloj: Reloj
) : ViewModel(){

    @Inject lateinit var tareasRepository: TareasRepository
    @Inject lateinit var eventosRepository: EventosRepository
    val _tareas= MutableLiveData<List<Tarea>>()
    val _eventos= MutableLiveData<List<Evento>>()
    val _listAgenda= MutableLiveData<List<AgendaItem>>()

    fun obtenerAgendaList() {
        var list :List<AgendaItem> = emptyList()
        obtenerEventos()
        obtenerTareas()
        if (_tareas.value?.isNotEmpty() == true){
            // Solo lo pendiente: las tareas entregadas o calificadas no aparecen
            list = _tareas.value!!.filterNot { it.completada }.map {
                AgendaItem(id = it.id,titulo = it.titulo, descrip = it.descrip, asignatura = it.asignatura, fecha = it.fecha, prioridad = it.prioridad)
            }
        }
        if (_eventos.value?.isNotEmpty() == true){
            val dos: List<AgendaItem> = _eventos.value!!.filterNot { eventoTerminado(it) }.map {
                AgendaItem(id = it.id,titulo = it.titulo, descrip = it.descrip, asignatura = null, fecha = it.fecha, prioridad = it.prioridad)
            }
            list = list.plus(dos)
        }
        _listAgenda.value=list
    }
    /** Un evento se da por completado cuando pasa su hora de fin (o su día, si no tiene hora de fin). */
    private fun eventoTerminado(evento: Evento): Boolean {
        val dia = runCatching { LocalDate.parse(evento.fecha) }.getOrNull() ?: return false
        val horaFin = runCatching { LocalTime.parse(evento.horaFin) }.getOrNull()
        val fin = if (horaFin != null) LocalDateTime.of(dia, horaFin) else dia.plusDays(1).atStartOfDay()
        return !reloj.ahora().toLocalDateTime().isBefore(fin)
    }

    fun deleteTarea(agendaItem: AgendaItem){
        val tarea= Tarea(agendaItem.id,agendaItem.titulo,agendaItem.descrip,agendaItem.asignatura!!,agendaItem.fecha, 1)
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
        val evento = Evento(agendaItem.id,agendaItem.titulo,agendaItem.descrip,agendaItem.fecha, 1)
        deleteEvento(evento)
    }

}