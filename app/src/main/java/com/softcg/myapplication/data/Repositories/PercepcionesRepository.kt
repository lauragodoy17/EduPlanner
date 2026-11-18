package com.softcg.myapplication.data.Repositories

import com.softcg.myapplication.data.database.dao.PercepcionesDao
import com.softcg.myapplication.data.database.entities.PercepcionEntity
import com.softcg.myapplication.data.network.SesionUsuario
import javax.inject.Inject

/** Interés, dificultad y método que declara el estudiante, siempre de su propio uid. */
class PercepcionesRepository @Inject constructor(
    private val dao: PercepcionesDao,
    private val sesion: SesionUsuario
) {
    suspend fun guardar(
        tipo: String, origen: String, asignaturaId: Int, tema: String?, valor: Float?, texto: String?, fechaMillis: Long
    ): Boolean {
        val uid = sesion.uid() ?: return false
        dao.guardar(PercepcionEntity(uid, tipo, origen, asignaturaId, tema, valor, texto, fechaMillis))
        return true
    }

    suspend fun deAsignatura(asignaturaId: Int): List<PercepcionEntity> {
        val uid = sesion.uid() ?: return emptyList()
        return dao.deAsignatura(uid, asignaturaId)
    }

    suspend fun existe(tipo: String, origen: String): Boolean {
        val uid = sesion.uid() ?: return false
        return dao.contar(uid, tipo, origen) > 0
    }
}
