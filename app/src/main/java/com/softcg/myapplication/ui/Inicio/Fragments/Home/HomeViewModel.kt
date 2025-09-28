package com.softcg.myapplication.ui.Inicio.Fragments.Home

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.ui.Inicio.Models.DateFilterItem
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.log

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTareasUseCase: getTareasUseCase,
    private val getEventosUseCase: getEventosUseCase,
) : ViewModel(){

    @Inject lateinit var tareasRepository: TareasRepository
    @Inject lateinit var eventosRepository: EventosRepository
    val _tareas= MutableLiveData<List<Tarea>>()
    val _eventos= MutableLiveData<List<Evento>>()
    val _filteredTareas= MutableLiveData<List<Tarea>>()
    val _filteredEventos= MutableLiveData<List<Evento>>()
    val _dateFilterItems= MutableLiveData<List<DateFilterItem>>()

    private var allTareas: List<Tarea> = emptyList()
    private var allEventos: List<Evento> = emptyList()
    private var selectedDate: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun obtenerEventos (){
        viewModelScope.launch {
            allEventos = getEventosUseCase()
            _eventos.value = allEventos
            filterDataByDate()
        }
    }

    fun obtenerTareas (){
        viewModelScope.launch {
            allTareas = getTareasUseCase()
            _tareas.value = allTareas
            filterDataByDate()
        }
    }

    fun initializeDateFilter() {
        val dateItems = generateDateFilterItems()
        _dateFilterItems.value = dateItems

        val today = dateFormat.format(Date())
        selectedDate = today
        filterDataByDate()
    }

    fun selectDate(dateItem: DateFilterItem) {
        selectedDate = dateFormat.format(dateItem.date)
        filterDataByDate()
    }

    private fun generateDateFilterItems(): List<DateFilterItem> {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        val todayString = dateFormat.format(today)

        calendar.add(Calendar.DAY_OF_YEAR, -15)

        val dateItems = mutableListOf<DateFilterItem>()

        for (i in 0..30) {
            val date = calendar.time
            val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
            val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

            val dayNumber = dayFormat.format(date)
            val dayName = dayNameFormat.format(date).uppercase()
            val dateString = dateFormat.format(date)
            val isToday = dateString == todayString

            dateItems.add(
                DateFilterItem(
                    date = date,
                    dayNumber = dayNumber,
                    dayName = dayName,
                    isToday = isToday
                )
            )

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return dateItems
    }

    private fun filterDataByDate() {
        Log.d("MiTag", "hola")
        Log.d("MiTag", selectedDate.toString())
        if(allTareas.getOrNull(0) != null){
            Log.d("MiTag", allTareas[0].fecha)
        }
        selectedDate?.let { date ->
            _filteredTareas.value = allTareas.filter { it.fecha == date }
            _filteredEventos.value = allEventos.filter { it.fecha == date }
        } ?: run {
            _filteredTareas.value = allTareas
            _filteredEventos.value = allEventos
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