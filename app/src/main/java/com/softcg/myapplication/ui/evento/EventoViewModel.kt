package com.softcg.myapplication.ui.evento

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.ui.evento.model.Evento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventoViewModel @Inject constructor( ) : ViewModel() {

    @Inject lateinit var eventosRepository: EventosRepository

    private val _navigateToHome = MutableLiveData<Event<Boolean>>()
    val navigateToHome: LiveData<Event<Boolean>>
        get()=_navigateToHome

    fun onAgregarEventoSelected(titulo:String,descrip:String, fecha:String,){
        val evento: Evento = Evento(null,titulo, descrip,fecha)
        saveEvento(evento)
    }

    fun onBackSelected(){
        _navigateToHome.value= Event(true)
    }

    fun saveEvento(evento: Evento){
        viewModelScope.launch {
            eventosRepository.insertEvento(evento)
        }
    }
}