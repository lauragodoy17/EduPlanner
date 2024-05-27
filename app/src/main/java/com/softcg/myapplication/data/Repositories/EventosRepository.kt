package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.EventosDao
import com.softcg.myapplication.data.database.entities.EventoEntity
import com.softcg.myapplication.ui.Inicio.Models.Evento
import javax.inject.Inject

class EventosRepository @Inject constructor(private val eventosDao: EventosDao) {

    fun getAllEventos():List<Evento>{
        val entities=eventosDao.getAllEventos()
        return entities.map {
            Evento(id = it.id, titulo = it.titulo, descrip = it.descrip, fecha = it.fecha)
        }
    }

    suspend fun deleteEvento(evento: Evento){
        val entity=EventoEntity(id = evento.id, titulo = evento.titulo, descrip = evento.descrip, fecha = evento.fecha)
        eventosDao.deleteEvento(entity)
    }

    suspend fun insertEvento(evento: Evento){
        val entity = EventoEntity(titulo = evento.titulo, descrip = evento.descrip, fecha = evento.fecha)
        eventosDao.insertEvento(entity)
    }
}