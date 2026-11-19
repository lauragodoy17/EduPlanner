package com.softcg.myapplication.ui.compartir

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softcg.myapplication.core.Event
import com.softcg.myapplication.domain.compartir.AnalisisImportacion
import com.softcg.myapplication.domain.compartir.CodigoQr
import com.softcg.myapplication.domain.compartir.ContenidoCompartido
import com.softcg.myapplication.domain.compartir.DestinoAsignatura
import com.softcg.myapplication.domain.compartir.ImportarCompartidoUseCase
import com.softcg.myapplication.domain.compartir.LecturaQr
import com.softcg.myapplication.domain.compartir.ResultadoImportacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** De un QR leído a lo agregado: primero se analiza (para la ventana de confirmación) y solo luego se importa. */
@HiltViewModel
class CompartirViewModel @Inject constructor(private val importarCompartido: ImportarCompartidoUseCase) : ViewModel() {

    private val _analisis = MutableLiveData<Event<AnalisisImportacion>>()
    val analisis: LiveData<Event<AnalisisImportacion>>
        get() = _analisis

    private val _resultado = MutableLiveData<Event<ResultadoImportacion>>()
    val resultado: LiveData<Event<ResultadoImportacion>>
        get() = _resultado

    /** El lector solo devuelve códigos válidos, pero se vuelve a comprobar: el texto podría venir de otro sitio. */
    fun onQrLeido(texto: String) {
        val lectura = CodigoQr.leer(texto) as? LecturaQr.Valida ?: return
        _analisis.value = Event(importarCompartido.analizar(lectura.contenido))
    }

    fun onAgregar(contenido: ContenidoCompartido, destino: DestinoAsignatura?) {
        viewModelScope.launch {
            _resultado.value = Event(importarCompartido.importar(contenido, destino))
        }
    }
}
