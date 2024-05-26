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
        initRecyclerTareas(view)
        initRecyclerEventos(view)
        return view
    }

    fun initRecyclerTareas(view: View){
        homeViewModel.obtenerTareas()
        val adapter = TareasAdapter(requireContext(),homeViewModel)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerclases)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeViewModel._tareas.observe(viewLifecycleOwner, Observer { Tarea ->
            adapter.setData(Tarea)
            if (Tarea.isNotEmpty()) {
                view.findViewById<TextView>(R.id.textonoclases).visibility = View.GONE
            } else{
                view.findViewById<TextView>(R.id.textonoclases).visibility = View.VISIBLE
            }
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

        homeViewModel._eventos.observe(viewLifecycleOwner, Observer { Evento ->
            adapter.setData(Evento)
            if (Evento.isNotEmpty()) {
                view.findViewById<TextView>(R.id.textonoeventos).visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.textonoeventos).visibility = View.VISIBLE
            }
        })
    }
    fun act(){
        homeViewModel.obtenerTareas()
    }


}


