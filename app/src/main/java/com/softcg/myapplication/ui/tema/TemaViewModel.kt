package com.softcg.myapplication.ui.tema

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.data.preferencias.PreferenciasTema
import com.softcg.myapplication.domain.preferencias.TemaApp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Opciones > Tema de la app: guarda el elegido y avisa para volver a crear la pantalla con él. */
@HiltViewModel
class TemaViewModel @Inject constructor(private val preferencias: PreferenciasTema) : ViewModel() {

    val temas: List<TemaApp> = TemaApp.entries

    fun temaActual(): TemaApp = preferencias.tema()

    /** Se emite solo si el tema cambió: elegir el que ya está no recarga nada. */
    private val _aplicar = MutableLiveData<Event<TemaApp>>()
    val aplicar: LiveData<Event<TemaApp>>
        get() = _aplicar

    fun elegir(tema: TemaApp) {
        if (tema == preferencias.tema()) return
        preferencias.guardar(tema)
        _aplicar.value = Event(tema)
    }
}
