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
import com.softcg.myapplication.domain.compartir.ContenidoCompartido
import com.softcg.myapplication.ui.Inicio.Dialogs.CompartirQrDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.TareaDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.configurarAccionesDetalle
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesDatosEstudio
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesTarea
import com.softcg.myapplication.ui.Inicio.InicioViewModel
import com.softcg.myapplication.ui.Inicio.Dialogs.pintarDatos
import com.softcg.myapplication.ui.Inicio.Dialogs.pintarEstadoTarea
import com.softcg.myapplication.ui.Inicio.Dialogs.pintarInfoTarea
import com.softcg.myapplication.ui.Inicio.Dialogs.pintarRecomendacion
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import dagger.hilt.android.AndroidEntryPoint

/**
 * Pantalla de una tarea (desde su tarjeta): toda su información y, debajo, "Predicción y recomendaciones". Muestra qué datos reales del estudiante tiene el modelo y cuáles
 * faltan (con la acción para completarlos); cuando están todos, predice y muestra las recomendaciones de la IA.
 */
@AndroidEntryPoint
class PrediccionFragment : Fragment() {

    private val prediccionViewModel: PrediccionViewModel by viewModels()
    private val datosEstudio: DatosEstudioViewModel by activityViewModels()
    private val acciones by lazy { AccionesDatosEstudio(this, datosEstudio) }
    private val inicioViewModel: InicioViewModel by activityViewModels()
    private val accionesTarea by lazy {
        AccionesTarea(
            this, inicioViewModel, acciones,
            onBorrar = { prediccionViewModel.borrar() },
            // Tras entregar o calificar se vuelve a leer la tarea (estado, nota) y sus datos
            onCambio = { prediccionViewModel.cargar(tareaId) }
        )
    }

    private val tareaId: Int get() = requireArguments().getInt(ARG_TAREA_ID, -1)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_prediccion, container, false)
        initTarea(view)
        initDatos(view)
        initRecomendacion(view)
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
        // En la pantalla el título va alineado con la etiqueta (el include lo centra)
        view.findViewById<TextView>(R.id.tareaTitulo).textAlignment = View.TEXT_ALIGNMENT_VIEW_START

        // Entregar o calificar sigue el mismo flujo que en Inicio y Agenda (recompensa, nota, dificultad)
        accionesTarea.observarResultados()
        val entregar = view.findViewById<MaterialButton>(R.id.tareaEntregar)
        prediccionViewModel.tarea.observe(viewLifecycleOwner) { tarea ->
            tarea ?: return@observe
            estado.pintarEstadoTarea(tarea)
            pintarInfoTarea(info, tarea)
            // El botón siempre está: entregar, luego calificar y, ya calificada, solo informa
            val (texto, accion) = when {
                tarea.calificacion != null -> "Entregada y calificada" to null
                tarea.entregadaMillis != null -> "Calificar tarea" to accionesTarea::calificar
                else -> "Entregar tarea" to accionesTarea::entregar
            }
            entregar.text = texto
            entregar.isEnabled = accion != null
            entregar.setOnClickListener { accion?.invoke(tarea) }
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
            onGuardar = { titulo, descrip, asignatura, fecha, prioridad, horaEntrega ->
                prediccionViewModel.editar(tarea.copy(titulo = titulo, descrip = descrip, asignatura = asignatura, fecha = fecha, prioridad = prioridad, horaEntrega = horaEntrega))
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

    /** Plan de estudio de la IA (Gemini) hecho con el resultado del modelo; el botón cambia según el paso. */
    private fun initRecomendacion(view: View) {
        val tarjeta = view.findViewById<View>(R.id.prediccionIa)
        val generando = view.findViewById<View>(R.id.iaGenerando)
        val generandoTexto = view.findViewById<TextView>(R.id.iaGenerandoTexto)
        val contenido = view.findViewById<View>(R.id.iaContenido)
        val error = view.findViewById<TextView>(R.id.iaError)
        val boton = view.findViewById<MaterialButton>(R.id.prediccionEnviar)

        val pintarBoton = {
            val prediciendo = prediccionViewModel.estado.value == EstadoPrediccion.Cargando
            val ia = prediccionViewModel.recomendacion.value
            boton.isEnabled = !prediciendo && ia != EstadoRecomendacion.Generando
            boton.text = when {
                prediciendo -> "Consultando el modelo…"
                ia == EstadoRecomendacion.Generando -> "Generando recomendaciones…"
                ia is EstadoRecomendacion.Lista -> "Generar otras recomendaciones"
                ia is EstadoRecomendacion.Error -> "Reintentar"
                else -> "Generar recomendaciones"
            }
        }
        prediccionViewModel.estado.observe(viewLifecycleOwner) {
            pintarBoton()
            generandoTexto.text = if (it == EstadoPrediccion.Cargando) "Consultando el modelo predictivo con tus datos…"
            else "Preparando tu plan de estudio con el resultado del modelo…"
        }
        prediccionViewModel.recomendacion.observe(viewLifecycleOwner) { estado ->
            pintarBoton()
            tarjeta.visibility = if (estado == null) View.GONE else View.VISIBLE
            generando.visibility = if (estado == EstadoRecomendacion.Generando) View.VISIBLE else View.GONE
            error.visibility = View.GONE
            when (estado) {
                null, EstadoRecomendacion.Generando -> contenido.visibility = View.GONE
                is EstadoRecomendacion.Lista -> {
                    // Aquí solo se predice con todos los datos: no hace falta el enlace "Completar mis datos"
                    pintarRecomendacion(tarjeta, estado.recomendacion, prediccionViewModel.tarea.value?.titulo.orEmpty())
                }
                is EstadoRecomendacion.Error -> {
                    // Si había un plan anterior se deja a la vista
                    error.text = estado.mensaje
                    error.visibility = View.VISIBLE
                }
            }
        }
    }

    companion object {
        const val ARG_TAREA_ID = "tarea_id"
        const val ARG_TITULO = "titulo"
        const val ARG_ASIGNATURA = "asignatura"
    }
}
