package com.softcg.myapplication.ui.tarea

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.ui.tarea.model.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TareaViewModel @Inject constructor() :ViewModel() {

    @Inject lateinit var tareasRepository:TareasRepository

    private val _navigateToHome = MutableLiveData<Event<Boolean>>()
    val navigateToHome:LiveData<Event<Boolean>>
        get()=_navigateToHome

    fun onAgregarTareaSelected(titulo:String,descrip:String,asignatura:String,fecha:String){
        val tarea:Tarea = Tarea(null,titulo, descrip, asignatura,fecha)
        saveTarea(tarea)
    }

    fun onBackSelected(){
        _navigateToHome.value=Event(true)
    }

    fun saveTarea(tarea:Tarea){
        viewModelScope.launch {
            tareasRepository.insertTarea(tarea)
        }
    }

}