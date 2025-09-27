package com.softcg.myapplication.ui.Inicio.Fragments.Home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ):
            View? {
        val view = inflater.inflate(layout.fragment_home, container, false)
        initDateFilter(view)
        initRecyclerTareas(view)
        initRecyclerEventos(view)
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

    fun initRecyclerEventos(view: View){
        homeViewModel.obtenerEventos()

        val adapter = EventosAdapter(requireContext(),homeViewModel)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclereventos)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        homeViewModel._filteredEventos.observe(viewLifecycleOwner, Observer { eventos ->
            adapter.setData(eventos)
            updateVisibility(view)
        })
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



}


