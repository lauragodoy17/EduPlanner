package com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones

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
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Adapters.CalificacionesAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RatingsFragment : Fragment() {

    private val calificacionesViewModel:CalificacionesViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view=inflater.inflate(R.layout.fragment_ratings, container, false)
        initRecyclerCalificaciones(view)
        return view
    }

    private fun initRecyclerCalificaciones(view: View){
        calificacionesViewModel.obtenerCalificaciones()
        val adapter = CalificacionesAdapter(requireContext(),calificacionesViewModel)
        val recyclerView= view.findViewById<RecyclerView>(R.id.recyclercalificaciones)
        recyclerView.adapter=adapter
        recyclerView.layoutManager=LinearLayoutManager(requireContext())
        calificacionesViewModel._calificaciones.observe(viewLifecycleOwner, Observer { Calificacion ->
            adapter.setData(Calificacion)
            if (Calificacion.isNotEmpty()){
                view.findViewById<TextView>(R.id.textoCalificaciones).visibility=View.GONE
            }else{
                view.findViewById<TextView>(R.id.textoCalificaciones).visibility = View.VISIBLE
            }
        })
    }


}