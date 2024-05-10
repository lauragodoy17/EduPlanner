package com.softcg.myapplication.ui.home.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.home.ViewModelHome
import com.softcg.myapplication.ui.home.adapters.AgendaAdapter
import com.softcg.myapplication.ui.home.adapters.TareasAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiaryFragment : Fragment() {

    private val viewModelHome: ViewModelHome by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_diary, container, false)
        initRecyclerAgenda(view)
        return view
    }

    fun initRecyclerAgenda(view: View){
        viewModelHome.obtenerTareas()
        viewModelHome.obtenerEventos()
        viewModelHome.obtenerAgendaList()
        val adapter = AgendaAdapter(requireContext(),viewModelHome)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycleragenda)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        viewModelHome._listAgenda.observe(viewLifecycleOwner, Observer {AgendaItem ->
            adapter.setData(AgendaItem)
            if (AgendaItem.isNotEmpty()) {
                view.findViewById<TextView>(R.id.textoAgenda).visibility = View.GONE
            } else{
                view.findViewById<TextView>(R.id.textoAgenda).visibility = View.VISIBLE
            }
        })
    }


}