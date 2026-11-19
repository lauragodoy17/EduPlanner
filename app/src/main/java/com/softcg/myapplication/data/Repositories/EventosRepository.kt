package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.DuenoDatosLocales
import com.softcg.myapplication.data.database.dao.EventosDao
import com.softcg.myapplication.data.database.entities.EventoEntity
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.ui.Inicio.Models.Evento
import javax.inject.Inject

class EventosRepository @Inject constructor(
    private val eventosDao: EventosDao,
    private val sesion: SesionUsuario,
    private val dueno: DuenoDatosLocales
) {

    private fun uid(): String? = sesion.uid()?.also { dueno.asegurar(it) }

    fun getAllEventos():List<Evento>{
        val uid = uid() ?: return emptyList()
        return eventosDao.getAllEventos(uid).map {
            Evento(id = it.id, titulo = it.titulo, descrip = it.descrip, fecha = it.fecha, prioridad = it.prioridad, horaInicio = it.horaInicio, horaFin = it.horaFin, imagenUri = it.imagenUri)
        }
    }

    suspend fun getEvento(id: Int): Evento? {
        val uid = uid() ?: return null
        return eventosDao.getEvento(uid, id)?.let {
            Evento(id = it.id, titulo = it.titulo, descrip = it.descrip, fecha = it.fecha, prioridad = it.prioridad, horaInicio = it.horaInicio, horaFin = it.horaFin, imagenUri = it.imagenUri)
        }
    }

    suspend fun deleteEvento(evento: Evento){
        val uid = uid() ?: return
        eventosDao.deleteEvento(evento.toEntity(uid))
    }

    /** Devuelve el id del evento creado (-1 sin sesión). */
    suspend fun insertEvento(evento: Evento): Long{
        val uid = uid() ?: return -1
        return eventosDao.insertEvento(evento.copy(id = null).toEntity(uid))
    }

    suspend fun updateEvento(evento: Evento){
        val uid = uid() ?: return
        eventosDao.updateEvento(evento.toEntity(uid))
    }

    private fun Evento.toEntity(uid: String) = EventoEntity(
        id = id, titulo = titulo, descrip = descrip, fecha = fecha, prioridad = prioridad,
        horaInicio = horaInicio, horaFin = horaFin, imagenUri = imagenUri, uid = uid
    )
}
