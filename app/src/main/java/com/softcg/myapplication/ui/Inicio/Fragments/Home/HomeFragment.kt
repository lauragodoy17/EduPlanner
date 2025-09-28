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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.R.*
import com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters.DateFilterAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters.EventosAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters.TareasAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters.TimelineAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters.UnifiedItemAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var unifiedAdapter: UnifiedItemAdapter
    private var currentFilter = 0 // 0: Todo, 1: Solo Tareas, 2: Solo Eventos

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ):
            View? {
        val view = inflater.inflate(layout.fragment_home, container, false)
        initDateFilter(view)
        initFilterSpinner(view)
        initTimeline(view)
        initRecyclerTareas(view)
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
    }

    fun initFilterSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.filter_spinner)
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filter_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
            onItemClick = { item -> /* Handle item click */ }
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
    }

    private fun updateTimeline(adapter: UnifiedItemAdapter, view: View) {
        val tareas = homeViewModel._filteredTareas.value ?: emptyList()
        val eventos = homeViewModel._filteredEventos.value ?: emptyList()

        adapter.setData(tareas, eventos)
        updateTimelineVisibility(view, tareas.isNotEmpty() || eventos.isNotEmpty())
        updateVisibility(view)
    }

    private fun updateTimelineVisibility(view: View, hasItems: Boolean) {
        val timelineContainer = view.findViewById<View>(R.id.recycler_timeline).parent as View
        timelineContainer.visibility = if (hasItems) View.VISIBLE else View.GONE
    }

    fun initRecyclerTareas(view: View){
        homeViewModel.obtenerTareas()
        val adapter = TareasAdapter(requireContext(),homeViewModel)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerclases)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        homeViewModel._filteredTareas.observe(viewLifecycleOwner, Observer { tareas ->
            adapter.setData(tareas)
            updateVisibility(view)
        })
    }

    fun actualizar(){
        homeViewModel.obtenerTareas()
    }

    fun act(){
        homeViewModel.obtenerTareas()
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
            0 -> { // Todo
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


