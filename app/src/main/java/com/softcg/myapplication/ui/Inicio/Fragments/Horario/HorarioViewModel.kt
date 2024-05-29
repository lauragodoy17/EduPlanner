package com.softcg.myapplication .ui.Inicio.Fragments.Horario

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HorarioViewModel  @Inject constructor(
    private val getAsignaturasUseCase: getAsignaturasUseCase
): ViewModel(){

    val _currentDate= MutableLiveData<LocalDate>()
    val _asignaturas= MutableLiveData<List<Asignatura>>()

    fun obtenerAsignaturas (){
        var todas:List<Asignatura> = listOf(Asignatura(null,"","",1, listOf(false,false,false,false,false,false,false)))
        viewModelScope.launch {
            todas = getAsignaturasUseCase()
        }
        filtrarAsignaturas(todas,_currentDate.value)
    }

    fun obtenerCurrentDate(currentDate: LocalDate){
        _currentDate.value=currentDate
    }

    private fun filtrarAsignaturas(todas:List<Asignatura>, currentDate: LocalDate?){
        val aux= mutableListOf<Asignatura>()
        if (currentDate?.dayOfWeek?.value ==1){
            for (asignatura in todas){
                if (asignatura.horario[0]){
                    aux.add(asignatura)
                }
            }
        }else if (currentDate?.dayOfWeek?.value==2){
            for (asignatura in todas){
                if (asignatura.horario[1]){
                    aux.add(asignatura)
                }
            }
        }else if (currentDate?.dayOfWeek?.value==3){
            for (asignatura in todas){
                if (asignatura.horario[2]){
                    aux.add(asignatura)
                }
            }
        }else if (currentDate?.dayOfWeek?.value==4){
            for (asignatura in todas){
                if (asignatura.horario[3]){
                    aux.add(asignatura)
                }
            }
        }else if (currentDate?.dayOfWeek?.value==5){
            for (asignatura in todas){
                if (asignatura.horario[4]){
                    aux.add(asignatura)
                }
            }
        }else if (currentDate?.dayOfWeek?.value==6){
            for (asignatura in todas){
                if (asignatura.horario[5]){
                    aux.add(asignatura)
                }
            }
        }else if (currentDate?.dayOfWeek?.value==7){
            for (asignatura in todas){
                if (asignatura.horario[6]){
                    aux.add(asignatura)
                }
            }
        }
        _asignaturas.value=aux
    }

}