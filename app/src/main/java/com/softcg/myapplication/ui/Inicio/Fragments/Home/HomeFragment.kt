package com.softcg.myapplication.ui.Inicio.Fragments.Home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.softcg.myapplication.R
import com.softcg.myapplication.R.*
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.core.ex.mostrarRecompensa
import com.softcg.myapplication.domain.gamificacion.ResultadoCalificarTarea
import com.softcg.myapplication.domain.gamificacion.ResultadoEntregarTarea
import com.softcg.myapplication.ui.Inicio.Dialogs.CalificarTareaDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.EventoDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.TareaDetallesDialog
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesDatosEstudio
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import com.softcg.myapplication.domain.ia.RecomendarTareaUseCase
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import com.softcg.myapplication.ui.Inicio.InicioViewModel
import com.softcg.myapplication.ui.Inicio.Models.Evento
import com.softcg.myapplication.ui.Inicio.Models.TimelineItem
import com.softcg.myapplication.ui.Inicio.Models.TimelineItemType
import com.softcg.myapplication.ui.Inicio.Fragments.Prediccion.PrediccionFragment
import androidx.core.os.bundleOf
import com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters.DateFilterAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters.UnifiedItemAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val inicioViewModel: InicioViewModel by activityViewModels()
    private val datosEstudio: DatosEstudioViewModel by activityViewModels()
    private val acciones by lazy { AccionesDatosEstudio(this, datosEstudio) }
    @Inject lateinit var recomendarTarea: RecomendarTareaUseCase
    private lateinit var unifiedAdapter: UnifiedItemAdapter
    private var currentFilter = 0 // 0: Todos, 1: Solo Tareas, 2: Solo Eventos

    /** Tarea que se está entregando o calificando: al terminar se pregunta su dificultad. */
    private var tareaEnCurso: Tarea? = null

    private var eventoDialog: EventoDialog? = null
    private val seleccionarImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { eventoDialog?.mostrarImagen(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ):
            View? {
        val view = inflater.inflate(layout.fragment_home, container, false)
        initDateFilter(view)
        initFilterSpinner(view)
        initTimeline(view)
        initResumenProgreso(view)
        return view
    }

    fun initDateFilter(view: View) {
        val dateFilterAdapter = DateFilterAdapter { dateItem ->
            homeViewModel.selectDate(dateItem)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_date_filter)
        recyclerView.adapter = dateFilterAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        homeViewModel.initializeDateFilter()

        homeViewModel._dateFilterItems.observe(viewLifecycleOwner, Observer { dateItems ->
            dateFilterAdapter.setData(dateItems)

            val todayPosition = dateItems.indexOfFirst { it.isToday }
            if (todayPosition != -1) {
                recyclerView.scrollToPosition(todayPosition)
            }
        })

        // Mantiene marcado el día que muestra la lista (p. ej. al crear un evento para otro día)
        homeViewModel.fechaSeleccionada.observe(viewLifecycleOwner) { fecha ->
            view.findViewById<TextView>(R.id.homeFechaTitulo).text = tituloDeFecha(fecha)
            val posicion = homeViewModel.posicionDeFecha(fecha)
            dateFilterAdapter.selectDate(posicion)
            if (posicion != -1) recyclerView.scrollToPosition(posicion)
        }
    }

    /** "Hoy" o, para otro día, "Martes 4 de noviembre". */
    private fun tituloDeFecha(fecha: String): String {
        val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        if (fecha == entrada.format(Date())) return "Hoy"
        val dia = runCatching { entrada.parse(fecha) }.getOrNull() ?: return fecha
        return SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(dia).replaceFirstChar { it.uppercase() }
    }

    fun initFilterSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.filter_spinner)
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filter_options,
            R.layout.item_spinner_pastilla
        )
        adapter.setDropDownViewResource(R.layout.item_menu_asignatura)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = position
                updateTimelineWithFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
    }

    fun initTimeline(view: View) {
        homeViewModel.obtenerTareas()
        homeViewModel.obtenerEventos()

        unifiedAdapter = UnifiedItemAdapter(
            context = requireContext(),
            onDeleteTarea = { tarea -> homeViewModel.deleteTarea(tarea) },
            onDeleteEvento = { evento -> homeViewModel.deleteEvento(evento) },
            onEditEvento = { evento -> showDialogEditarEvento(evento) },
            onCalificarTarea = { tarea ->
                CalificarTareaDialog(requireContext(), tarea) { texto ->
                    tareaEnCurso = tarea
                    inicioViewModel.calificarTarea(tarea, texto)
                }.show()
            },
            onEntregarTarea = { tarea ->
                tareaEnCurso = tarea
                inicioViewModel.entregarTarea(tarea)
            },
            onVerMasTarea = { tarea ->
                TareaDetallesDialog(
                    requireContext(), tarea, viewLifecycleOwner.lifecycleScope, recomendarTarea,
                    onCompletarDatos = { abrirPrediccion(it.id, it.titulo, it.asignatura) }
                ).show()
            },
            // Los eventos abren su detalle desde el propio adaptador
            onItemClick = { item -> if (item.type == TimelineItemType.TAREA) abrirPrediccion(item.id, item.titulo, item.asignatura) }
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_timeline)
        recyclerView.adapter = unifiedAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe both tareas and eventos to update timeline
        homeViewModel._filteredTareas.observe(viewLifecycleOwner, Observer {
            updateTimelineWithFilter()
        })

        homeViewModel._filteredEventos.observe(viewLifecycleOwner, Observer {
            updateTimelineWithFilter()
        })

        inicioViewModel.resultadoCalificarTarea.observe(viewLifecycleOwner) { evento ->
            when (val resultado = evento.getContentIfNotHandled() ?: return@observe) {
                is ResultadoCalificarTarea.Completada -> {
                    homeViewModel.obtenerTareas()
                    if (resultado.monedas > 0) requireActivity().mostrarRecompensa(resultado.monedas, if (resultado.aTiempo) "A tiempo" else null)
                    else requireActivity().mostrarMensaje("Tarea completada")
                    tareaEnCurso?.let { acciones.preguntarDificultadDeTarea(it) }
                    tareaEnCurso = null
                }
                ResultadoCalificarTarea.YaCalificada -> requireActivity().mostrarMensaje("Esta tarea ya tiene una calificación")
                ResultadoCalificarTarea.ValorInvalido -> requireActivity().mostrarMensaje("Ingresa una calificación válida")
                ResultadoCalificarTarea.Error -> requireActivity().mostrarMensaje("No se pudo guardar la calificación. Inténtalo de nuevo")
            }
        }

        // Un tema elegido desde "Ver más" cambia la tarea: se recarga la lista
        datosEstudio.cambios.observe(viewLifecycleOwner) { homeViewModel.obtenerTareas() }

        inicioViewModel.resultadoEntregarTarea.observe(viewLifecycleOwner) { evento ->
            when (val resultado = evento.getContentIfNotHandled() ?: return@observe) {
                is ResultadoEntregarTarea.Entregada -> {
                    homeViewModel.obtenerTareas()
                    if (resultado.monedas > 0) requireActivity().mostrarRecompensa(resultado.monedas, if (resultado.aTiempo) "A tiempo" else null)
                    else requireActivity().mostrarMensaje("Tarea entregada")
                    tareaEnCurso?.let { acciones.preguntarDificultadDeTarea(it) }
                    tareaEnCurso = null
                }
                ResultadoEntregarTarea.YaEntregada -> requireActivity().mostrarMensaje("Esta tarea ya estaba entregada")
                ResultadoEntregarTarea.Error -> requireActivity().mostrarMensaje("No se pudo marcar la tarea. Inténtalo de nuevo")
            }
        }
    }

    private fun initResumenProgreso(view: View) {
        view.findViewById<View>(R.id.homeProgreso).setOnClickListener {
            findNavController().navigate(R.id.id_profile_fragment)
        }
        inicioViewModel.panelProgreso.observe(viewLifecycleOwner) { panel ->
            val misiones = panel.misiones
            view.findViewById<TextView>(R.id.homeProgresoNivel).text = panel.nivel.nivel.numero.toString()
            view.findViewById<TextView>(R.id.homeProgresoNombre).text = panel.nivel.nivel.nombre
            view.findViewById<LinearProgressIndicator>(R.id.homeProgresoBarra)
                .setProgressCompat((panel.nivel.fraccion * 100).toInt(), true)
            view.findViewById<TextView>(R.id.homeProgresoRacha).text = panel.racha.actual.toString()
            view.findViewById<TextView>(R.id.homeProgresoMisiones).text =
                if (misiones.porReclamar > 0) "${misiones.porReclamar} por reclamar"
                else "${misiones.diariasReclamadas}/${misiones.diarias.size} hoy"
        }
    }


    private fun abrirPrediccion(tareaId: Int?, titulo: String, asignatura: String?) {
        findNavController().navigate(
            R.id.id_prediction_fragment,
            bundleOf(
                PrediccionFragment.ARG_TAREA_ID to (tareaId ?: -1),
                PrediccionFragment.ARG_TITULO to titulo,
                PrediccionFragment.ARG_ASIGNATURA to asignatura
            )
        )
    }

    private fun showDialogEditarEvento(evento: Evento) {
        eventoDialog = EventoDialog(
            context = requireContext(),
            evento = evento,
            onSeleccionarImagen = { seleccionarImagen.launch("image/*") },
            onGuardar = { actualizado ->
                inicioViewModel.onActualizarEventoSelected(
                    actualizado.id,
                    actualizado.titulo,
                    actualizado.descrip,
                    actualizado.fecha,
                    actualizado.prioridad,
                    actualizado.horaInicio,
                    actualizado.horaFin,
                    actualizado.imagenUri
                )
                homeViewModel.mostrarFecha(actualizado.fecha)
                requireView().mostrarAviso("Evento actualizado")
            }
        ).also { it.show() }
    }

    private fun updateVisibility(view: View) {
        val hasTareas = homeViewModel._filteredTareas.value?.isNotEmpty() ?: false
        val hasEventos = homeViewModel._filteredEventos.value?.isNotEmpty() ?: false

        if (hasTareas || hasEventos) {
            // Show content version
            view.findViewById<View>(R.id.version_a_layout).visibility = View.VISIBLE
            view.findViewById<View>(R.id.version_b_layout).visibility = View.GONE
        } else {
            // Show empty state
            view.findViewById<View>(R.id.version_a_layout).visibility = View.GONE
            view.findViewById<View>(R.id.version_b_layout).visibility = View.VISIBLE
        }
    }

    private fun updateTimelineWithFilter() {
        if (!::unifiedAdapter.isInitialized) return

        val allTareas = homeViewModel._filteredTareas.value ?: emptyList()
        val allEventos = homeViewModel._filteredEventos.value ?: emptyList()

        when (currentFilter) {
            0 -> { // Todos
                unifiedAdapter.setData(allTareas, allEventos)
            }
            1 -> { // Solo Tareas
                unifiedAdapter.setData(allTareas, emptyList())
            }
            2 -> { // Solo Eventos
                unifiedAdapter.setData(emptyList(), allEventos)
            }
        }

        // Update visibility based on filtered content
        view?.let { updateVisibility(it) }
    }



}


