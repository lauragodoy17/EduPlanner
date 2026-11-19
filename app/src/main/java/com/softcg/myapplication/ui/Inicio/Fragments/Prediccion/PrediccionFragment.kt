package com.softcg.myapplication.ui.Inicio.Fragments.Prediccion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.softcg.myapplication.R
import androidx.navigation.fragment.findNavController
import com.softcg.myapplication.core.ex.confirmar
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.domain.compartir.ContenidoCompartido
import com.softcg.myapplication.ui.Inicio.Dialogs.CompartirQrDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.TareaDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.configurarAccionesDetalle
import com.softcg.myapplication.data.network.PrediccionService
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesDatosEstudio
import com.softcg.myapplication.ui.Inicio.Dialogs.pintarDatos
import com.softcg.myapplication.ui.Inicio.Dialogs.pintarEstadoTarea
import com.softcg.myapplication.ui.Inicio.Dialogs.pintarInfoTarea
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pantalla de una tarea (desde su tarjeta): toda su información y, debajo, "Predicción y recomendaciones". Muestra qué datos reales del estudiante tiene el modelo y cuáles
 * faltan (con la acción para completarlos); cuando están todos, envía la petición y muestra la respuesta.
 */
@AndroidEntryPoint
class PrediccionFragment : Fragment() {

    private val prediccionViewModel: PrediccionViewModel by viewModels()
    private val datosEstudio: DatosEstudioViewModel by activityViewModels()
    private val acciones by lazy { AccionesDatosEstudio(this, datosEstudio) }

    private val tareaId: Int get() = requireArguments().getInt(ARG_TAREA_ID, -1)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_prediccion, container, false)
        initTarea(view)
        initDatos(view)
        initRespuesta(view)
        // Cada dato que se completa desde esta pantalla vuelve a extraer los datos completos
        datosEstudio.cambios.observe(viewLifecycleOwner) { prediccionViewModel.cargar(tareaId) }
        return view
    }

    override fun onResume() {
        super.onResume()
        // Al volver de Enfoque o Calificaciones los datos pueden haber cambiado
        prediccionViewModel.cargar(tareaId)
    }

    private fun initTarea(view: View) {
        val estado = view.findViewById<TextView>(R.id.tareaEstado)
        val info = view.findViewById<View>(R.id.prediccionInfoTarea)
        // Mientras se lee la tarea guardada se muestra lo que trajo la tarjeta
        val args = requireArguments()
        val provisional = Tarea(tareaId, args.getString(ARG_TITULO).orEmpty(), "", args.getString(ARG_ASIGNATURA).orEmpty(), "", 0)
        estado.pintarEstadoTarea(provisional)
        pintarInfoTarea(info, provisional)
        // En la pantalla el título va alineado con la etiqueta; en la hoja "Ver más", centrado
        view.findViewById<TextView>(R.id.tareaTitulo).textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        prediccionViewModel.tarea.observe(viewLifecycleOwner) { tarea ->
            tarea ?: return@observe
            estado.pintarEstadoTarea(tarea)
            pintarInfoTarea(info, tarea)
        }

        configurarAccionesDetalle(
            view.findViewById(R.id.tareaAcciones),
            onEditar = ::editar,
            onBorrar = ::confirmarBorrar,
            onCompartir = {
                prediccionViewModel.tarea.value?.let { CompartirQrDialog(requireContext(), ContenidoCompartido.de(it)).show() }
            }
        )
        prediccionViewModel.mensaje.observe(viewLifecycleOwner) { it.getContentIfNotHandled()?.let { texto -> view.mostrarAviso(texto) } }
        prediccionViewModel.borrada.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { texto ->
                requireActivity().mostrarMensaje(texto)
                findNavController().popBackStack()
            }
        }
    }

    private fun editar() {
        val tarea = prediccionViewModel.tarea.value ?: return
        TareaDialog(
            context = requireContext(),
            asignaturas = prediccionViewModel.nombresAsignaturas(),
            onAgregarAsignatura = null,
            tarea = tarea,
            onGuardar = { titulo, descrip, asignatura, fecha, prioridad ->
                prediccionViewModel.editar(tarea.copy(titulo = titulo, descrip = descrip, asignatura = asignatura, fecha = fecha, prioridad = prioridad))
            }
        ).show()
    }

    private fun confirmarBorrar() {
        prediccionViewModel.tarea.value ?: return
        requireContext().confirmar("¿Eliminar la tarea?", "Esta acción no se puede deshacer.", "Eliminar") {
            prediccionViewModel.borrar()
        }
    }

    private fun initDatos(view: View) {
        val resumen = view.findViewById<TextView>(R.id.prediccionResumen)
        val progreso = view.findViewById<LinearProgressIndicator>(R.id.prediccionProgreso)
        val contenedor = view.findViewById<android.widget.LinearLayout>(R.id.prediccionDatos)
        val avisos = view.findViewById<TextView>(R.id.prediccionAvisos)
        val enviar = view.findViewById<MaterialButton>(R.id.prediccionEnviar)
        enviar.setOnClickListener { prediccionViewModel.predecirDeNuevo() }

        prediccionViewModel.datos.observe(viewLifecycleOwner) { estado ->
            val resultado = (estado as? EstadoDatos.Listos)?.resultado
            progreso.visibility = if (resultado != null) View.VISIBLE else View.GONE
            avisos.visibility = View.GONE
            enviar.visibility = if (resultado?.datos != null) View.VISIBLE else View.GONE
            contenedor.removeAllViews()
            when (estado) {
                EstadoDatos.SinTarea -> resumen.text = "Esta tarea ya no existe."
                EstadoDatos.SinSesion -> resumen.text = "Inicia sesión para ver tus predicciones."
                EstadoDatos.SinAsignatura -> resumen.text =
                    "La tarea no pertenece a ninguna asignatura registrada. Registra la asignatura en Asignaturas para reunir sus datos."
                is EstadoDatos.Listos -> {
                    val r = estado.resultado
                    resumen.text = if (r.completo) "Tus ${r.total} datos están listos."
                    else "${r.listos} de ${r.total} datos listos. Completa los que faltan para predecir."
                    progreso.setProgressCompat(r.listos * 100 / r.total.coerceAtLeast(1), false)
                    pintarDatos(contenedor, r.estados) { dato -> acciones.ejecutar(dato, r) }
                    if (r.avisos.isNotEmpty()) {
                        avisos.text = r.avisos.joinToString("\n")
                        avisos.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun initRespuesta(view: View) {
        val envio = view.findViewById<View>(R.id.prediccionEnvio)
        val etiqueta = view.findViewById<TextView>(R.id.prediccionEstado)
        val carga = view.findViewById<LinearProgressIndicator>(R.id.prediccionCarga)
        val respuesta = view.findViewById<TextView>(R.id.prediccionRespuesta)
        view.findViewById<TextView>(R.id.prediccionEndpoint).text =
            "POST ${PrediccionService.URL_BASE}${PrediccionService.RUTA_PREDICT}?top_n=${prediccionViewModel.topN}"

        prediccionViewModel.estado.observe(viewLifecycleOwner) { estado ->
            envio.visibility = if (estado == null) View.GONE else View.VISIBLE
            if (estado == null) return@observe
            view.findViewById<TextView>(R.id.prediccionEnviado).text =
                prediccionViewModel.datosEnviados?.let { formatearJson(it.aJson()) }.orEmpty()
            carga.visibility = if (estado is EstadoPrediccion.Cargando) View.VISIBLE else View.GONE
            when (estado) {
                EstadoPrediccion.Cargando -> {
                    etiqueta.pintarEtiqueta("ENVIANDO", R.color.acento, R.color.marca_superficie)
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
        const val ARG_TAREA_ID = "tarea_id"
        const val ARG_TITULO = "titulo"
        const val ARG_ASIGNATURA = "asignatura"
    }
}
