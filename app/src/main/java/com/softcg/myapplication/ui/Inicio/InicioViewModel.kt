package com.softcg.myapplication.ui.Inicio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.evento.model.Evento
import com.softcg.myapplication.ui.Inicio.Models.AgendaItem
import com.softcg.myapplication.ui.Inicio.Models.Asignatura
import com.softcg.myapplication.ui.tarea.model.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InicioViewModel @Inject constructor() : ViewModel(){

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
}