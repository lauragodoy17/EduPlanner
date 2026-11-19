package com.softcg.myapplication.ui.Inicio.Fragments.Detalle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.Repositories.AsignaturasRepository
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.domain.EditarAsignaturaUseCase
import com.softcg.myapplication.domain.NuevaAsignatura
import com.softcg.myapplication.domain.ResultadoEdicion
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Evento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Lo que muestra la pantalla de detalle, leído de la base de datos (no lo que traía la tarjeta). */
sealed class ElementoDetalle {
    data class DeEvento(val evento: Evento) : ElementoDetalle()
    data class DeAsignatura(val asignatura: Asignatura) : ElementoDetalle()
    data class DeNota(val calificacion: Calificacion) : ElementoDetalle()
    /** Se borró (p. ej. desde otro dispositivo o pantalla) o no hay sesión. */
    object NoExiste : ElementoDetalle()
}

/** Pantalla de un evento, asignatura o calificación: la carga, la edita y la borra. */
@HiltViewModel
class DetalleViewModel @Inject constructor(
    private val eventos: EventosRepository,
    private val asignaturas: AsignaturasRepository,
    private val calificaciones: CalificacionesRespository,
    private val editarAsignatura: EditarAsignaturaUseCase
) : ViewModel() {

    private val _elemento = MutableLiveData<ElementoDetalle>()
    val elemento: LiveData<ElementoDetalle>
        get() = _elemento

    /** Aviso breve tras guardar o si algo falla. */
    private val _mensaje = MutableLiveData<Event<String>>()
    val mensaje: LiveData<Event<String>>
        get() = _mensaje

    /** Se borró: la pantalla se cierra con este aviso. */
    private val _borrado = MutableLiveData<Event<String>>()
    val borrado: LiveData<Event<String>>
        get() = _borrado

    private var tipo: TipoDetalle? = null
    private var id: Int = -1

    fun cargar(tipo: TipoDetalle, id: Int) {
        this.tipo = tipo
        this.id = id
        recargar()
    }

    private fun recargar() {
        val tipo = tipo ?: return
        viewModelScope.launch {
            _elemento.value = when (tipo) {
                TipoDetalle.EVENTO -> eventos.getEvento(id)?.let { ElementoDetalle.DeEvento(it) }
                TipoDetalle.ASIGNATURA -> asignaturas.getAsignatura(id)?.let { ElementoDetalle.DeAsignatura(it) }
                TipoDetalle.NOTA -> calificaciones.getCalificacion(id)?.let { ElementoDetalle.DeNota(it) }
            } ?: ElementoDetalle.NoExiste
        }
    }

    fun nombresAsignaturas(): List<String> = asignaturas.getAllAsignaturas().map { it.nombre }

    fun actualizarEvento(evento: Evento) {
        viewModelScope.launch {
            eventos.updateEvento(evento.copy(id = id))
            _mensaje.value = Event("Evento actualizado")
            recargar()
        }
    }

    fun actualizarAsignatura(datos: NuevaAsignatura) {
        viewModelScope.launch {
            _mensaje.value = Event(
                when (editarAsignatura(id, datos)) {
                    ResultadoEdicion.Guardada -> "Asignatura actualizada"
                    ResultadoEdicion.NombreVacio -> "Escribe el nombre de la asignatura"
                    ResultadoEdicion.NombreRepetido -> "Ya tienes otra asignatura con ese nombre"
                    ResultadoEdicion.NoExiste -> "No se pudo guardar: la asignatura ya no existe"
                }
            )
            recargar()
        }
    }

    fun actualizarCalificacion(calificacion: Calificacion) {
        viewModelScope.launch {
            val ok = calificaciones.updateCalificacion(calificacion.copy(id = id))
            _mensaje.value = Event(if (ok) "Calificación actualizada" else "No se pudo guardar la calificación")
            recargar()
        }
    }

    fun borrar() {
        val actual = _elemento.value ?: return
        viewModelScope.launch {
            val aviso = when (actual) {
                is ElementoDetalle.DeEvento -> eventos.deleteEvento(actual.evento).let { "Evento eliminado" }
                is ElementoDetalle.DeAsignatura -> asignaturas.deleteAsignatura(actual.asignatura).let { "Asignatura eliminada" }
                is ElementoDetalle.DeNota -> calificaciones.deleteCalificacion(actual.calificacion).let { "Calificación eliminada" }
                ElementoDetalle.NoExiste -> return@launch
            }
            _borrado.value = Event(aviso)
        }
    }
}
