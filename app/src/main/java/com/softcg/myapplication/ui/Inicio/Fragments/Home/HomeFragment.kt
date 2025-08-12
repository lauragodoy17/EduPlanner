package com.softcg.myapplication.ui.Inicio.Fragments.Home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
        setupHamburgerMenu(view)
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

        homeViewModel._eventos.observe(viewLifecycleOwner, Observer { Evento ->
            adapter.setData(Evento)
            updateVisibility(view)
        })
    }
    fun act(){
        homeViewModel.obtenerTareas()
    }

    private fun updateVisibility(view: View) {
        val hasTareas = homeViewModel._tareas.value?.isNotEmpty() ?: false
        val hasEventos = homeViewModel._eventos.value?.isNotEmpty() ?: false
        
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

    private fun setupHamburgerMenu(view: View) {
        val hamburgerMenu = view.findViewById<ImageView>(R.id.menu_hamburger)
        hamburgerMenu?.setOnClickListener {
            try {
                val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer)
                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}


