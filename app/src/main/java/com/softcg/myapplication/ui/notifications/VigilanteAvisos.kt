package com.softcg.myapplication.ui.notifications

import android.util.Log
import androidx.room.InvalidationTracker
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import com.softcg.myapplication.domain.notificaciones.ReprogramarAvisosUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Replanifica los avisos cuando cambian las tablas de las que dependen, sin que cada pantalla tenga que acordarse:
 * crear, editar, entregar o borrar una tarea; cambiar una asignatura; responder una asistencia.
 * Varios cambios seguidos (p. ej. entregar y calificar) se juntan en una sola replanificación.
 */
@Singleton
class VigilanteAvisos @Inject constructor(
    private val db: TareasDatabase,
    private val reprogramar: ReprogramarAvisosUseCase
) {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendiente: Job? = null
    private var iniciado = false

    private val observador = object : InvalidationTracker.Observer(TABLAS) {
        override fun onInvalidated(tables: Set<String>) = solicitar()
    }

    @Synchronized
    fun iniciar() {
        if (iniciado) return
        iniciado = true
        db.invalidationTracker.addObserver(observador)
    }

    /** Replanifica en un momento; llamadas seguidas dentro de ese margen cuentan como una. */
    @Synchronized
    fun solicitar() {
        pendiente?.cancel()
        pendiente = alcance.launch {
            delay(ESPERA_MS)
            runCatching { reprogramar() }.onFailure { Log.w("VigilanteAvisos", "No se pudieron replanificar los avisos", it) }
        }
    }

    /** Al cerrar sesión: sin estudiante no hay avisos. */
    fun cancelarTodo() {
        pendiente?.cancel()
        reprogramar.cancelarTodo()
    }

    private companion object {
        val TABLAS = arrayOf("tareas_table", "asignaturas_table", "asistencias", "calificaciones_table")
        const val ESPERA_MS = 800L
    }
}
