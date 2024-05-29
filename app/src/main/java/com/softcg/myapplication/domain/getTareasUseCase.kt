package com.softcg.myapplication.domain

import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import javax.inject.Inject

class getTareasUseCase @Inject constructor(private val repository: TareasRepository) {
    operator fun invoke():List<Tarea>{
        return repository.getTareas()
    }
}

class getEventosUseCase @Inject constructor(private val repository: EventosRepository) {
    operator fun invoke():List<Evento>{
        return repository.getAllEventos()
    }
}

class getAsignaturasUseCase @Inject constructor(private val repository: AsignaturasRepository) {
    operator fun invoke():List<Asignatura>{
        return repository.getAllAsignaturas()
    }
}