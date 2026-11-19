package com.softcg.myapplication.ui.Inicio.Fragments.Detalle

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Prediccion.PrediccionFragment

/** Abre la pantalla de un evento, asignatura o calificación guardada. */
fun Fragment.abrirDetalle(tipo: TipoDetalle, id: Int?) {
    id ?: return
    findNavController().navigate(
        R.id.id_detail_fragment,
        bundleOf(DetalleFragment.ARG_TIPO to tipo.clave, DetalleFragment.ARG_ID to id)
    )
}

/** Abre la pantalla de una tarea: su información y "Predicción y recomendaciones". */
fun Fragment.abrirTarea(id: Int?, titulo: String, asignatura: String?) {
    findNavController().navigate(
        R.id.id_prediction_fragment,
        bundleOf(
            PrediccionFragment.ARG_TAREA_ID to (id ?: -1),
            PrediccionFragment.ARG_TITULO to titulo,
            PrediccionFragment.ARG_ASIGNATURA to asignatura
        )
    )
}

/** Conecta los tres iconos de `include_acciones_detalle` dentro de [raiz]. */
fun configurarAccionesDetalle(raiz: View, onEditar: () -> Unit, onBorrar: () -> Unit, onCompartir: () -> Unit) {
    raiz.findViewById<View>(R.id.btnDetalleEditar).setOnClickListener { onEditar() }
    raiz.findViewById<View>(R.id.btnDetalleBorrar).setOnClickListener { onBorrar() }
    raiz.findViewById<View>(R.id.btnDetalleCompartir).setOnClickListener { onCompartir() }
}
