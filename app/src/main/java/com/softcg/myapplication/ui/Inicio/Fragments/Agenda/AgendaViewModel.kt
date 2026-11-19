package com.softcg.myapplication.ui.Inicio.Fragments.Agenda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.AgendaItem
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.FiltroAgenda
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

    /** Filtro de arriba a la derecha; empieza en "Sin entregar" y se conserva al volver a la agenda. */
    private val _filtro = MutableLiveData(FiltroAgenda.SIN_ENTREGAR)
    val filtro: LiveData<FiltroAgenda>
        get() = _filtro

    fun elegirFiltro(filtro: FiltroAgenda) {
        if (_filtro.value == filtro) return
        _filtro.value = filtro
        obtenerAgendaList()
    }

    fun obtenerAgendaList() {
        obtenerEventos()
        obtenerTareas()
        _listAgenda.value = elementos(
            _tareas.value.orEmpty(), _eventos.value.orEmpty(), _filtro.value ?: FiltroAgenda.SIN_ENTREGAR
        )
    }

    /**
     * Lo que muestra cada filtro: "Sin entregar" = tareas sin entregar + eventos que no han pasado; "Solo tareas" =
     * tareas sin entregar; "Solo eventos" = eventos que no han pasado; "Entregados" = tareas entregadas o calificadas.
     */
    internal fun elementos(tareas: List<Tarea>, eventos: List<Evento>, filtro: FiltroAgenda): List<AgendaItem> {
        val tareasElegidas = when (filtro) {
            FiltroAgenda.SIN_ENTREGAR, FiltroAgenda.SOLO_TAREAS -> tareas.filterNot { it.completada }
            FiltroAgenda.ENTREGADOS -> tareas.filter { it.completada }
            FiltroAgenda.SOLO_EVENTOS -> emptyList()
        }
        val eventosElegidos = when (filtro) {
            FiltroAgenda.SIN_ENTREGAR, FiltroAgenda.SOLO_EVENTOS -> eventos.filterNot { eventoTerminado(it) }
            else -> emptyList()
        }
        return tareasElegidas.map {
            AgendaItem(
                id = it.id, titulo = it.titulo, descrip = it.descrip, asignatura = it.asignatura, fecha = it.fecha,
                prioridad = it.prioridad, entregada = it.completada, textoCalificacion = it.textoCalificacion
            )
        } + eventosElegidos.map {
            AgendaItem(id = it.id, titulo = it.titulo, descrip = it.descrip, asignatura = null, fecha = it.fecha, prioridad = it.prioridad)
        }
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