package com.softcg.myapplication.ui.Inicio.Fragments.Home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.evento.model.Evento
import com.softcg.myapplication.ui.tarea.model.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTareasUseCase: getTareasUseCase,
    private val getEventosUseCase: getEventosUseCase,
) : ViewModel(){

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
        }
    }

    fun deleteEvento(evento: Evento){
        viewModelScope.launch {
            eventosRepository.deleteEvento(evento)
            obtenerEventos()
        }
    }



}