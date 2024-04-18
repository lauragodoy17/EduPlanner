package com.softcg.myapplication.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.softcg.myapplication.core.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewModelHome @Inject constructor() : ViewModel(){

    private val _navigateToTarea= MutableLiveData<Event<Boolean>>()
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