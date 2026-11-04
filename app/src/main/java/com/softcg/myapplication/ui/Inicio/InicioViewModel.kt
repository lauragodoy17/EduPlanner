package com.softcg.myapplication.ui.Inicio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.CalificacionesRespository
import com.softcg.myapplication.data.Repositories.EventosRepository
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.getAsignaturasUseCase
import com.softcg.myapplication.domain.getCalificacionesUseCase
import com.softcg.myapplication.domain.getEventosUseCase
import com.softcg.myapplication.domain.getTareasUseCase
import com.softcg.myapplication.domain.gamificacion.AsistenciaReglas
import com.softcg.myapplication.domain.gamificacion.CalificarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.Celebracion
import com.softcg.myapplication.domain.gamificacion.ClaseProgramada
import com.softcg.myapplication.domain.gamificacion.CondicionesRecompensa
import com.softcg.myapplication.domain.gamificacion.ConfirmarAsistenciaUseCase
import com.softcg.myapplication.domain.gamificacion.EntregarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.EstadoAsistencia
import com.softcg.myapplication.domain.gamificacion.EvaluarProgresoUseCase
import com.softcg.myapplication.domain.gamificacion.ObtenerClasesPorConfirmarUseCase
import com.softcg.myapplication.domain.gamificacion.ObtenerEnfoqueActivoUseCase
import com.softcg.myapplication.domain.gamificacion.ObtenerPanelProgresoUseCase
import com.softcg.myapplication.domain.gamificacion.OtorgarRecompensaUseCase
import com.softcg.myapplication.domain.gamificacion.PanelProgreso
import com.softcg.myapplication.domain.gamificacion.ResultadoAsistencia
import com.softcg.myapplication.domain.gamificacion.ResultadoCalificarTarea
import com.softcg.myapplication.domain.gamificacion.ResultadoEntregarTarea
import com.softcg.myapplication.domain.gamificacion.ResultadoRecompensa
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import com.softcg.myapplication.ui.Inicio.Models.Evento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val getTareasUseCase: getTareasUseCase,
    private val getEventosUseCase: getEventosUseCase,
    private val getAsignaturasUseCase: getAsignaturasUseCase,
    private val getCalificacionesUseCase: getCalificacionesUseCase,
    private val otorgarRecompensa: OtorgarRecompensaUseCase,
    private val calificarTareaUseCase: CalificarTareaUseCase,
    private val obtenerClasesPorConfirmar: ObtenerClasesPorConfirmarUseCase,
    private val confirmarAsistenciaUseCase: ConfirmarAsistenciaUseCase,
    private val entregarTareaUseCase: EntregarTareaUseCase,
    private val evaluarProgresoUseCase: EvaluarProgresoUseCase,
    private val obtenerPanel: ObtenerPanelProgresoUseCase,
    private val obtenerEnfoqueActivo: ObtenerEnfoqueActivoUseCase,
    private val reloj: Reloj,
    gamificacionRepository: GamificacionRepository,
    sesion: SesionUsuario
) : ViewModel(){

    @Inject lateinit var tareasRepository: TareasRepository
    @Inject lateinit var eventosRepository: EventosRepository
    @Inject lateinit var calificacionesRespository: CalificacionesRespository


    val _asignaturas = MutableLiveData<List<String>>()
    val _tareas = MutableLiveData<List<Tarea>>()
    val _eventos = MutableLiveData<List<Evento>>()
    val _calificaciones=MutableLiveData<List<Calificacion>>()

    // EDUCOINS
    val saldo: LiveData<Int> = sesion.uid()?.let { gamificacionRepository.observarSaldo(it) } ?: MutableLiveData(0)

    /** EduCoins ganados con la última operación guardada; solo se emite si el movimiento se registró. */
    private val _recompensa = MutableLiveData<Event<Int>>()
    val recompensa: LiveData<Event<Int>>
        get() = _recompensa

    private val _resultadoCalificarTarea = MutableLiveData<Event<ResultadoCalificarTarea>>()
    val resultadoCalificarTarea: LiveData<Event<ResultadoCalificarTarea>>
        get() = _resultadoCalificarTarea

    private val _resultadoEntregarTarea = MutableLiveData<Event<ResultadoEntregarTarea>>()
    val resultadoEntregarTarea: LiveData<Event<ResultadoEntregarTarea>>
        get() = _resultadoEntregarTarea

    // PROGRESO
    /** Cambia con cada movimiento, venga de la pantalla que venga. */
    val totalMovimientos: LiveData<Int> =
        sesion.uid()?.let { gamificacionRepository.observarTotalMovimientos(it) } ?: MutableLiveData(0)

    private val refrescoProgreso = MutableLiveData(Unit)

    val panelProgreso: LiveData<PanelProgreso> = MediatorLiveData<Unit>().apply {
        addSource(refrescoProgreso) { value = Unit }
        sesion.uid()?.let { addSource(gamificacionRepository.observarCambios(it)) { value = Unit } }
    }.switchMap {
        liveData { obtenerPanel()?.let { emit(it) } }
    }

    private val _celebraciones = MutableLiveData<Event<List<Celebracion>>>()
    val celebraciones: LiveData<Event<List<Celebracion>>>
        get() = _celebraciones

    // ASISTENCIA
    private val _resultadoAsistencia = MutableLiveData<Event<Pair<ClaseProgramada, ResultadoAsistencia>>>()
    val resultadoAsistencia: LiveData<Event<Pair<ClaseProgramada, ResultadoAsistencia>>>
        get() = _resultadoAsistencia

    /** Clases que el usuario decidió responder más tarde; vuelven a preguntarse al volver a la app. */
    private val pospuestas = mutableSetOf<Pair<Int, LocalDate>>()

    //HOME ACTIVITY
    private val _navigateToTarea = MutableLiveData<Event<Boolean>>()
    val navigateToTarea: LiveData<Event<Boolean>>
        get() = _navigateToTarea

    private val _navigateToEvento= MutableLiveData<Event<Boolean>>()
    val navigateToEvento: LiveData<Event<Boolean>>
        get() = _navigateToEvento

    private val _navigateToCalificacion= MutableLiveData<Event<Boolean>>()
    val navigateToCalificacion: LiveData<Event<Boolean>>
        get() = _navigateToCalificacion

    fun onTareaSelected(){
        _navigateToTarea.value= Event(true)
    }

    fun onEventoSelected(){
        _navigateToEvento.value= Event(true)
    }

    fun onAgregarTareaSelected(titulo:String,descrip:String,asignatura:String,fecha:String,prioridad:Int){
        val tarea: Tarea = Tarea(null,titulo, descrip, asignatura,fecha,prioridad)
        saveTarea(tarea)
    }

    fun obtenerTareas (){
        viewModelScope.launch {
            _tareas.value = getTareasUseCase()
        }
    }
    fun obtenerEventos (){
        viewModelScope.launch {
            _eventos.value = getEventosUseCase()
        }
    }

    fun obtenerAsignaturas(){
        viewModelScope.launch {
            _asignaturas.value = getAsignaturasUseCase().map { it.nombre }
        }
        if(_asignaturas.value?.isEmpty() == true){
            _asignaturas.value= listOf("Agregar Asignatura")
        }
    }
    fun onAgregarCalificacionSelected(tipo:String,valor:Float,asignatura:String,descripcion:String){
        val calificacion = Calificacion(null,tipo,valor,asignatura, descripcion)
        saveCalificacion(calificacion)
    }

    fun obtenerCalificaciones(){
        viewModelScope.launch {
            _calificaciones.value = getCalificacionesUseCase()
        }
    }
    // Las listas se recargan después de guardar: el insert de Room es asíncrono
    // y una recarga lanzada en paralelo leería los datos de antes.
    fun saveCalificacion(calificacion: Calificacion){
        viewModelScope.launch {
            val id = calificacionesRespository.insertCalificacion(calificacion)
            _calificaciones.value = getCalificacionesUseCase()
            if (CondicionesRecompensa.calificacionValida(calificacion.valor, calificacion.asignatura)) {
                premiar(TipoRecompensa.CALIFICACION_REGISTRADA, id)
            }
        }
    }

    fun saveTarea(tarea: Tarea){
        viewModelScope.launch {
            val id = tareasRepository.insertTarea(tarea)
            _tareas.value = getTareasUseCase()
            if (CondicionesRecompensa.tareaValida(tarea.titulo, tarea.fecha)) {
                premiar(TipoRecompensa.TAREA_REGISTRADA, id)
            }
        }
    }

    /** Registrar la calificación de una tarea la da por completada. */
    fun calificarTarea(tarea: Tarea, textoValor: String){
        viewModelScope.launch {
            val resultado = calificarTareaUseCase(tarea, textoValor)
            if (resultado is ResultadoCalificarTarea.Completada) {
                obtenerTareas()
                obtenerCalificaciones()
            }
            _resultadoCalificarTarea.value = Event(resultado)
        }
    }

    /** Marca la tarea como entregada sin esperar la nota. */
    fun entregarTarea(tarea: Tarea){
        viewModelScope.launch {
            val resultado = entregarTareaUseCase(tarea)
            if (resultado is ResultadoEntregarTarea.Entregada) {
                obtenerTareas()
            }
            _resultadoEntregarTarea.value = Event(resultado)
        }
    }

    fun refrescarProgreso() {
        refrescoProgreso.value = Unit
    }

    /** Una sesión de enfoque que ya terminó y falta cobrar: no se pierde por no abrir la notificación. */
    suspend fun hayEnfoquePorCobrar(): Boolean =
        obtenerEnfoqueActivo()?.let { reloj.ahora().toInstant().toEpochMilli() >= it.finMillis } ?: false

    /** Concede logros y niveles ganados; solo avisa de lo que se acaba de guardar. */
    fun evaluarProgreso() {
        viewModelScope.launch {
            val nuevas = evaluarProgresoUseCase()
            if (nuevas.isNotEmpty()) _celebraciones.value = Event(nuevas)
        }
    }

    fun onAgregarEventoSelected(titulo:String,descrip:String, fecha:String,prioridad:Int, horaInicio:String = "", horaFin:String = "", imagenUri:String? = null){
        val evento: Evento = Evento(null,titulo, descrip,fecha,prioridad, horaInicio, horaFin, imagenUri)
        saveEvento(evento)
    }
    fun saveEvento(evento: Evento){
        viewModelScope.launch {
            val id = eventosRepository.insertEvento(evento)
            _eventos.value = getEventosUseCase()
            if (CondicionesRecompensa.eventoValido(evento.titulo, evento.fecha)) {
                premiar(TipoRecompensa.EVENTO_REGISTRADO, id)
            }
        }
    }

    // Editar un evento no da recompensa
    fun updateEvento(evento: Evento){
        viewModelScope.launch {
            eventosRepository.updateEvento(evento)
            _eventos.value = getEventosUseCase()
        }
    }

    fun onActualizarEventoSelected(id: Int?, titulo:String, descrip:String, fecha:String, prioridad:Int, horaInicio:String = "", horaFin:String = "", imagenUri:String? = null){
        val evento: Evento = Evento(id, titulo, descrip, fecha, prioridad, horaInicio, horaFin, imagenUri)
        updateEvento(evento)
    }

    private suspend fun premiar(tipo: TipoRecompensa, id: Long) {
        val resultado = otorgarRecompensa(tipo, "$id")
        if (resultado is ResultadoRecompensa.Otorgada) {
            _recompensa.value = Event(resultado.cantidad)
        }
    }

    // ASISTENCIA

    /** La próxima clase que se puede confirmar ahora (la que terminó primero), sin contar las pospuestas. */
    suspend fun siguienteClasePorConfirmar(): ClaseProgramada? =
        obtenerClasesPorConfirmar().firstOrNull { (it.asignaturaId to it.fecha) !in pospuestas }

    fun plazoVencido(clase: ClaseProgramada): Boolean =
        AsistenciaReglas.estado(clase, null, reloj.ahora().toLocalDateTime()) == EstadoAsistencia.VENCIDA

    fun posponerAsistencia(clase: ClaseProgramada) {
        pospuestas += clase.asignaturaId to clase.fecha
    }

    fun reiniciarPospuestas() {
        pospuestas.clear()
    }

    fun confirmarAsistencia(clase: ClaseProgramada, asistio: Boolean) {
        viewModelScope.launch {
            val resultado = confirmarAsistenciaUseCase(clase.asignaturaId, clase.fecha, asistio)
            _resultadoAsistencia.value = Event(clase to resultado)
        }
    }
}
