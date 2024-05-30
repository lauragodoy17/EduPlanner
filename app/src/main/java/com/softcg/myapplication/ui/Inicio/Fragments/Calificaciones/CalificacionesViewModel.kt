package com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.domain.getCalificacionesUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.AgendaItem
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalificacionesViewModel @Inject constructor(
    private val getCalificacionesUseCase: getCalificacionesUseCase
) : ViewModel(){
    @Inject lateinit var calificacionesRespository: CalificacionesRespository

    val _calificaciones=MutableLiveData<List<Calificacion>>()

    fun obtenerCalificaciones(){
        viewModelScope.launch {
            _calificaciones.value = getCalificacionesUseCase()
        }
    }

    fun deleteCalificacion(calificacion: Calificacion){
        viewModelScope.launch {
            calificacionesRespository.deleteCalificacion(calificacion)
            obtenerCalificaciones()
        }
    }

    fun onAgregarCalificacionSelected(tipo:String,valor:Float,asignatura:String,descripcion:String){
        val calificacion=Calificacion(null,tipo,valor,asignatura,descripcion)
        viewModelScope.launch {
            calificacionesRespository.insertCalificacion(calificacion)
            obtenerCalificaciones()
        }
    }
}