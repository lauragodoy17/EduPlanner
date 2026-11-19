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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesDatosEstudio
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesTarea
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import com.softcg.myapplication.domain.ia.RecomendarTareaUseCase
import javax.inject.Inject
import com.softcg.myapplication.ui.Inicio.InicioViewModel
import com.softcg.myapplication.ui.Inicio.Models.TimelineItemType
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.TipoDetalle
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.abrirDetalle
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.abrirTarea
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

    private val accionesTarea by lazy {
        AccionesTarea(
            this, inicioViewModel, acciones, recomendarTarea,
            onBorrar = { homeViewModel.deleteTarea(it) },
            onCompletarDatos = { abrirTarea(it.id, it.titulo, it.asignatura) },
            onCambio = { homeViewModel.obtenerTareas() }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ):
            View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
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
            onCalificarTarea = accionesTarea::calificar,
            onEntregarTarea = accionesTarea::entregar,
            onVerMasTarea = accionesTarea::verDetalles,
            onItemClick = { item ->
                if (item.type == TimelineItemType.TAREA) abrirTarea(item.id, item.titulo, item.asignatura)
                else abrirDetalle(TipoDetalle.EVENTO, item.id)
            }
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


        // Entregar o calificar (desde la tarjeta o desde la tarea) recarga la lista y sigue el flujo de la tarea
        accionesTarea.observarResultados()

        // Un tema elegido desde "Ver más" cambia la tarea: se recarga la lista
        datosEstudio.cambios.observe(viewLifecycleOwner) { homeViewModel.obtenerTareas() }

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


