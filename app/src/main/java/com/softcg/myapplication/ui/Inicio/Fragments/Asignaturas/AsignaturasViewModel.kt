package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.ListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
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

    fun onAgregarAsignaturaSelected(nombre:String,tutor:String,duracion:Int, horario:List<ListItem>){
        val horarios=getHorario(horario)
        val asignatura = Asignatura(null,nombre,tutor,duracion,horarios)
        viewModelScope.launch {
            asignaturasRepository.insertAsignatura(asignatura)
            obtenerAsignaturas()
        }
    }
    private fun getHorario(items: List<ListItem>):List<Boolean>{
        val dias = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

        val resultado = MutableList(7) { false }

        for (item in items) {
            val dia = item.name.lowercase(Locale.getDefault())
            val index = dias.indexOf(dia)
            if (index != -1) {
                resultado[index] = true
            }
        }
        return resultado
    }


}