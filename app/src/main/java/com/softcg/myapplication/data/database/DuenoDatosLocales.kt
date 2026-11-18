package com.softcg.myapplication.data.database

import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hasta la versión 16 las tareas, eventos, asignaturas y calificaciones no tenían dueño: eran del dispositivo.
 * La migración las deja con uid '' y aquí las adopta el primer usuario que entra; desde entonces cada
 * usuario solo ve lo suyo. Se hace una vez por usuario y proceso, antes de la primera lectura.
 */
@Singleton
class DuenoDatosLocales @Inject constructor(private val db: TareasDatabase) {

    private val revisados = mutableSetOf<String>()

    fun asegurar(uid: String) {
        synchronized(revisados) {
            if (uid in revisados) return
            db.runInTransaction {
                db.getAsignaturasDao().adoptarSinDueno(uid)
                db.getTareasDao().adoptarSinDueno(uid)
                db.getEventosDao().adoptarSinDueno(uid)
                db.getCalificacionesDao().adoptarSinDueno(uid)
            }
            revisados += uid
        }
    }
}
