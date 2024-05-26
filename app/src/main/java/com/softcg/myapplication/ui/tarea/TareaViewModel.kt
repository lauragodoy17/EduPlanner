package com.softcg.myapplication.ui.tarea

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TareaViewModel @Inject constructor(
    private val getAsignaturasUseCase: getAsignaturasUseCase
) :ViewModel() {

    @Inject lateinit var tareasRepository:TareasRepository

    val _asignaturas = MutableLiveData<List<String>>()

    private val _navigateToHome = MutableLiveData<Event<Boolean>>()
    val navigateToHome:LiveData<Event<Boolean>>
        get()=_navigateToHome

    fun onAgregarTareaSelected(titulo:String,descrip:String,asignatura:String,fecha:String){
        val tarea: Tarea = Tarea(null,titulo, descrip, asignatura,fecha)
        saveTarea(tarea)
    }

    fun obtenerAsignaturas(){
        viewModelScope.launch {
            _asignaturas.value = getAsignaturasUseCase().map { it.nombre }
        }
        if(_asignaturas.value?.isEmpty() == true){
            _asignaturas.value= listOf("Agregar Asignatura")
        }
    }

    fun onBackSelected(){
        _navigateToHome.value=Event(true)
    }

    fun saveTarea(tarea: Tarea){
        viewModelScope.launch {
            tareasRepository.insertTarea(tarea)
        }
    }

}