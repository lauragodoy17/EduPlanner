package com.softcg.myapplication.ui.Inicio.Fragments.Prediccion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.data.network.PrediccionService
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONObject

/**
 * Prueba de la API de modelos desde una tarjeta de tarea: muestra lo que se envió y lo que respondió.
 * Por ahora siempre envía DatosPrediccion.PRUEBA, no los datos de la tarea.
 */
@AndroidEntryPoint
class PrediccionFragment : Fragment() {

    private val prediccionViewModel: PrediccionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_prediccion, container, false)
        initTarea(view)
        initEnviado(view)
        initRespuesta(view)
        prediccionViewModel.consultarSiHaceFalta()
        return view
    }

    private fun initTarea(view: View) {
        val args = requireArguments()
        view.findViewById<TextView>(R.id.prediccionTareaTitulo).text = args.getString(ARG_TITULO).orEmpty()
        val asignatura = args.getString(ARG_ASIGNATURA).orEmpty()
        view.findViewById<TextView>(R.id.prediccionTareaAsignatura).apply {
            text = asignatura
            visibility = if (asignatura.isBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun initEnviado(view: View) {
        view.findViewById<TextView>(R.id.prediccionEndpoint).text =
            "POST ${PrediccionService.URL_BASE}${PrediccionService.RUTA_PREDICT}?top_n=${prediccionViewModel.topN}"
        view.findViewById<TextView>(R.id.prediccionEnviado).text =
            formatearJson(prediccionViewModel.datosEnviados.aJson())
    }

    private fun initRespuesta(view: View) {
        val etiqueta = view.findViewById<TextView>(R.id.prediccionEstado)
        val carga = view.findViewById<LinearProgressIndicator>(R.id.prediccionCarga)
        val respuesta = view.findViewById<TextView>(R.id.prediccionRespuesta)

        prediccionViewModel.estado.observe(viewLifecycleOwner) { estado ->
            carga.visibility = if (estado is EstadoPrediccion.Cargando) View.VISIBLE else View.GONE
            when (estado) {
                EstadoPrediccion.Cargando -> {
                    etiqueta.pintarEtiqueta("ENVIANDO", R.color.purple_accent, R.color.purple_surface)
                    respuesta.text = "Esperando respuesta…"
                }
                is EstadoPrediccion.Respuesta -> {
                    if (estado.exitosa) etiqueta.pintarEtiqueta("HTTP ${estado.codigo}", R.color.estado_exito, R.color.estado_exito_suave)
                    else etiqueta.pintarEtiqueta("HTTP ${estado.codigo}", R.color.estado_error, R.color.estado_error_suave)
                    respuesta.text = formatearJson(estado.cuerpo).ifBlank { "(respuesta vacía)" }
                }
                is EstadoPrediccion.Error -> {
                    etiqueta.pintarEtiqueta("SIN RESPUESTA", R.color.estado_error, R.color.estado_error_suave)
                    respuesta.text = estado.mensaje
                }
            }
        }
    }

    /** JSON con sangría; si no es JSON (p. ej. una página de error de nginx) se muestra tal cual. */
    private fun formatearJson(texto: String): String = runCatching {
        val limpio = texto.trim()
        if (limpio.startsWith("[")) JSONArray(limpio).toString(2) else JSONObject(limpio).toString(2)
    }.getOrDefault(texto)

    companion object {
        const val ARG_TITULO = "titulo"
        const val ARG_ASIGNATURA = "asignatura"
    }
}
