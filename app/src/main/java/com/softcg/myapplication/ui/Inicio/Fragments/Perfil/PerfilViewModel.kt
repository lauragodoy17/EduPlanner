package com.softcg.myapplication.ui.Inicio.Fragments.Perfil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.network.SesionUsuario
import com.softcg.myapplication.domain.gamificacion.AvatarBase
import com.softcg.myapplication.domain.gamificacion.CambiarAparienciaUseCase
import com.softcg.myapplication.domain.gamificacion.CategoriaAccesorio
import com.softcg.myapplication.domain.gamificacion.ComprarAccesorioUseCase
import com.softcg.myapplication.domain.gamificacion.EquiparAccesorioUseCase
import com.softcg.myapplication.domain.gamificacion.MovimientoEduCoins
import com.softcg.myapplication.domain.gamificacion.ObtenerPanelProgresoUseCase
import com.softcg.myapplication.domain.gamificacion.PanelProgreso
import com.softcg.myapplication.domain.gamificacion.ReclamarMisionUseCase
import com.softcg.myapplication.domain.gamificacion.ResultadoCompra
import com.softcg.myapplication.domain.gamificacion.ResultadoEquipar
import com.softcg.myapplication.domain.gamificacion.ResultadoMision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Perfil y tienda leen saldo, historial, inventario y avatar de las mismas consultas de Room:
 * no hay copias propias que puedan desincronizarse.
 */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    repository: GamificacionRepository,
    sesion: SesionUsuario,
    private val comprarAccesorio: ComprarAccesorioUseCase,
    private val equiparAccesorio: EquiparAccesorioUseCase,
    private val cambiarApariencia: CambiarAparienciaUseCase,
    private val reclamarMisionUseCase: ReclamarMisionUseCase,
    private val obtenerPanel: ObtenerPanelProgresoUseCase,
    private val reloj: Reloj
) : ViewModel() {

    private val uid = sesion.uid()

    val email: String = sesion.email().orEmpty()

    val saldo: LiveData<Int> = uid?.let { repository.observarSaldo(it) } ?: MutableLiveData(0)
    val movimientos: LiveData<List<MovimientoEduCoins>> =
        uid?.let { repository.observarMovimientos(it) } ?: MutableLiveData(emptyList())
    val inventario: LiveData<Set<String>> =
        uid?.let { repository.observarInventario(it) } ?: MutableLiveData(emptySet())
    val equipados: LiveData<Map<CategoriaAccesorio, String>> =
        (uid?.let { repository.observarEquipados(it) } ?: MutableLiveData(emptyMap())).map { mapa ->
            mapa.mapNotNull { (categoria, itemId) ->
                runCatching { CategoriaAccesorio.valueOf(categoria) }.getOrNull()?.let { it to itemId }
            }.toMap()
        }

    /** Tonos elegidos por el usuario o, si aún no eligió, los derivados de su uid. */
    val avatarBase: LiveData<AvatarBase> =
        uid?.let { id ->
            repository.observarApariencia(id).map { guardada ->
                guardada?.let { AvatarBase(it.indicePiel, it.indicePelo) } ?: AvatarBase.desdeUid(id)
            }
        } ?: MutableLiveData(AvatarBase.desdeUid(""))

    private val refresco = MutableLiveData(Unit)

    /** Foto del progreso; se recalcula al cambiar los datos y al volver a la pantalla (pudo cambiar el día). */
    val panel: LiveData<PanelProgreso> = MediatorLiveData<Unit>().apply {
        addSource(refresco) { value = Unit }
        uid?.let { addSource(repository.observarCambios(it)) { value = Unit } }
    }.switchMap {
        liveData { obtenerPanel()?.let { emit(it) } }
    }

    val hoy: LocalDate get() = reloj.ahora().toLocalDate()

    private val _resultadoCompra = MutableLiveData<Event<ResultadoCompra>>()
    val resultadoCompra: LiveData<Event<ResultadoCompra>>
        get() = _resultadoCompra

    private val _resultadoEquipar = MutableLiveData<Event<ResultadoEquipar>>()
    val resultadoEquipar: LiveData<Event<ResultadoEquipar>>
        get() = _resultadoEquipar

    private val _resultadoMision = MutableLiveData<Event<ResultadoMision>>()
    val resultadoMision: LiveData<Event<ResultadoMision>>
        get() = _resultadoMision

    private val _aparienciaGuardada = MutableLiveData<Event<Boolean>>()
    val aparienciaGuardada: LiveData<Event<Boolean>>
        get() = _aparienciaGuardada

    fun refrescar() {
        refresco.value = Unit
    }

    fun comprar(itemId: String) {
        viewModelScope.launch {
            _resultadoCompra.value = Event(comprarAccesorio(itemId))
        }
    }

    fun equipar(itemId: String) {
        viewModelScope.launch {
            _resultadoEquipar.value = Event(equiparAccesorio.equipar(itemId))
        }
    }

    fun retirar(categoria: CategoriaAccesorio) {
        viewModelScope.launch {
            _resultadoEquipar.value = Event(equiparAccesorio.retirar(categoria))
        }
    }

    fun reclamarMision(misionId: String) {
        viewModelScope.launch {
            _resultadoMision.value = Event(reclamarMisionUseCase(misionId))
        }
    }

    fun elegirApariencia(base: AvatarBase) {
        viewModelScope.launch {
            _aparienciaGuardada.value = Event(cambiarApariencia(base))
        }
    }
}
