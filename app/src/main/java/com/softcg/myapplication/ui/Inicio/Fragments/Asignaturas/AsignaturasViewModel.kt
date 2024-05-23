package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Models.Asignatura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AsignaturasViewModel @Inject constructor(
    private val getAsignaturasUseCase: getAsignaturasUseCase
) : ViewModel(){

    @Inject lateinit var asignaturasRepository : AsignaturasRepository

    val _asignaturas= MutableLiveData<List<Asignatura>>()

    fun obtenerAsignaturas (){
        viewModelScope.launch {
            _asignaturas.value = getAsignaturasUseCase()
        }
    }
    fun deleteAsignatura(asignatura: Asignatura){
        viewModelScope.launch {
            asignaturasRepository.deleteAsignatura(asignatura)
            obtenerAsignaturas()
        }
    }

    fun onAgregarAsignaturaSelected(nombre:String,tutor:String){
        val asignatura: Asignatura = Asignatura(null,nombre,tutor)
        viewModelScope.launch {
            asignaturasRepository.insertAsignatura(asignatura)
        }
        obtenerAsignaturas()
    }

}