package com.softcg.myapplication.ui.Inicio.Fragments.Horario

import android.util.Log
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
class HorarioViewModel @Inject constructor(
    private val getAsignaturasUseCase: getAsignaturasUseCase
): ViewModel(){

    val _currentDate = MutableLiveData<LocalDate>()
    val _asignaturas = MutableLiveData<List<Asignatura>>()

    init {
        // Initialize with current date
        _currentDate.value = LocalDate.now()
        Log.d("HorarioViewModel", "Initialized with date: ${_currentDate.value}")
        // Fetch asignaturas on init
        obtenerAsignaturas()
    }

    fun obtenerAsignaturas(){
        viewModelScope.launch {
            try {
                val todas = getAsignaturasUseCase()
                Log.d("HorarioViewModel", "Fetched ${todas.size} asignaturas from database")
                todas.forEach { asig ->
                    Log.d("HorarioViewModel", "DB Asignatura: ${asig.nombre}, hora: ${asig.hora}, horario: ${asig.horario}")
                }
                filtrarAsignaturas(todas, _currentDate.value)
            } catch (e: Exception) {
                Log.e("HorarioViewModel", "Error fetching asignaturas", e)
                _asignaturas.value = emptyList()
            }
        }
    }

    fun obtenerCurrentDate(currentDate: LocalDate){
        _currentDate.value = currentDate
        Log.d("HorarioViewModel", "Date updated to: $currentDate, dayOfWeek: ${currentDate.dayOfWeek.value}")
        // Re-fetch and filter asignaturas when date changes
        obtenerAsignaturas()
    }

    private fun filtrarAsignaturas(todas: List<Asignatura>, currentDate: LocalDate?){
        if (currentDate == null) {
            Log.w("HorarioViewModel", "Current date is null")
            _asignaturas.value = emptyList()
            return
        }

        Log.d("HorarioViewModel", "═══════════════════════════════════════")
        Log.d("HorarioViewModel", "FILTERING ASIGNATURAS")
        Log.d("HorarioViewModel", "Date: $currentDate (${currentDate.dayOfWeek})")
        Log.d("HorarioViewModel", "Total asignaturas in database: ${todas.size}")

        // Map LocalDate.dayOfWeek to array index
        // Monday = 1 -> index 0, Tuesday = 2 -> index 1, ..., Sunday = 7 -> index 6
        val dayIndex = when (currentDate.dayOfWeek.value) {
            1 -> 0 // Lunes
            2 -> 1 // Martes
            3 -> 2 // Miércoles
            4 -> 3 // Jueves
            5 -> 4 // Viernes
            6 -> 5 // Sábado
            7 -> 6 // Domingo
            else -> {
                Log.w("HorarioViewModel", "Invalid day of week: ${currentDate.dayOfWeek.value}")
                _asignaturas.value = emptyList()
                return
            }
        }

        val diasSemana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        Log.d("HorarioViewModel", "Filtering for: ${diasSemana[dayIndex]} (index $dayIndex)")
        Log.d("HorarioViewModel", "───────────────────────────────────────")

        if (todas.isEmpty()) {
            Log.w("HorarioViewModel", "⚠ No asignaturas found in database!")
            Log.w("HorarioViewModel", "⚠ Please create asignaturas in the Asignaturas tab")
            _asignaturas.value = emptyList()
            return
        }

        val filtered = mutableListOf<Asignatura>()

        for (asignatura in todas) {
            val horarioStr = asignatura.horario.mapIndexed { idx, value ->
                "${diasSemana.getOrElse(idx) { "?" }}=$value"
            }.joinToString(", ")

            Log.d("HorarioViewModel", "Checking: ${asignatura.nombre}")
            Log.d("HorarioViewModel", "  - Hora: ${asignatura.hora}")
            Log.d("HorarioViewModel", "  - Duración: ${asignatura.duracion} horas")
            Log.d("HorarioViewModel", "  - Horario: [$horarioStr]")

            if (asignatura.horario.size != 7) {
                Log.w("HorarioViewModel", "  ✗ SKIPPED: Invalid horario size (expected 7, got ${asignatura.horario.size})")
                Log.w("HorarioViewModel", "  ⚠ This asignatura has corrupted data!")
                continue
            }

            if (asignatura.horario[dayIndex]) {
                filtered.add(asignatura)
                Log.d("HorarioViewModel", "  ✓ ADDED for ${diasSemana[dayIndex]}")
            } else {
                Log.d("HorarioViewModel", "  ✗ SKIPPED: Not scheduled for ${diasSemana[dayIndex]}")
            }
        }

        // Sort asignaturas by hora (time)
        val sortedFiltered = filtered.sortedBy { asignatura ->
            try {
                // Clean the time string thoroughly
                val hora = asignatura.hora.trim().replace(" ", "")
                val parts = hora.split(":")

                if (parts.size >= 2) {
                    val hour = parts[0].trim().toIntOrNull()
                    val minute = parts[1].trim().toIntOrNull()

                    if (hour != null && minute != null) {
                        hour * 60 + minute
                    } else {
                        Log.w("HorarioViewModel", "Could not parse time for ${asignatura.nombre}: '${asignatura.hora}' -> hour=$hour, minute=$minute")
                        Int.MAX_VALUE
                    }
                } else {
                    Log.w("HorarioViewModel", "Invalid time format for ${asignatura.nombre}: '${asignatura.hora}'")
                    Int.MAX_VALUE
                }
            } catch (e: Exception) {
                Log.e("HorarioViewModel", "Error parsing hora for ${asignatura.nombre}: '${asignatura.hora}'", e)
                Int.MAX_VALUE
            }
        }

        Log.d("HorarioViewModel", "───────────────────────────────────────")
        Log.d("HorarioViewModel", "FILTER RESULT:")
        Log.d("HorarioViewModel", "Total asignaturas for ${diasSemana[dayIndex]}: ${sortedFiltered.size}")
        if (sortedFiltered.isNotEmpty()) {
            Log.d("HorarioViewModel", "Schedule for ${diasSemana[dayIndex]}:")
            sortedFiltered.forEach {
                Log.d("HorarioViewModel", "  → ${it.hora} - ${it.nombre} (${it.duracion}h)")
            }
        } else {
            Log.d("HorarioViewModel", "No classes scheduled for ${diasSemana[dayIndex]}")
        }
        Log.d("HorarioViewModel", "═══════════════════════════════════════")

        _asignaturas.value = sortedFiltered
    }

}