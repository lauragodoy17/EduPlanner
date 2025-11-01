package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.EventosDao
import com.softcg.myapplication.data.database.entities.EventoEntity
import com.softcg.myapplication.ui.Inicio.Models.Evento
import javax.inject.Inject

class EventosRepository @Inject constructor(private val eventosDao: EventosDao) {

    fun getAllEventos():List<Evento>{
        val entities=eventosDao.getAllEventos()
        return entities.map {
            Evento(id = it.id, titulo = it.titulo, descrip = it.descrip, fecha = it.fecha, prioridad = it.prioridad, horaInicio = it.horaInicio, horaFin = it.horaFin, imagenUri = it.imagenUri)
        }
    }

    suspend fun deleteEvento(evento: Evento){
        val entity=EventoEntity(id = evento.id, titulo = evento.titulo, descrip = evento.descrip, fecha = evento.fecha, prioridad = evento.prioridad, horaInicio = evento.horaInicio, horaFin = evento.horaFin, imagenUri = evento.imagenUri)
        eventosDao.deleteEvento(entity)
    }

    suspend fun insertEvento(evento: Evento){
        val entity = EventoEntity(titulo = evento.titulo, descrip = evento.descrip, fecha = evento.fecha, prioridad = evento.prioridad, horaInicio = evento.horaInicio, horaFin = evento.horaFin, imagenUri = evento.imagenUri)
        eventosDao.insertEvento(entity)
    }

    suspend fun updateEvento(evento: Evento){
        val entity = EventoEntity(id = evento.id, titulo = evento.titulo, descrip = evento.descrip, fecha = evento.fecha, prioridad = evento.prioridad, horaInicio = evento.horaInicio, horaFin = evento.horaFin, imagenUri = evento.imagenUri)
        eventosDao.updateEvento(entity)
    }
}