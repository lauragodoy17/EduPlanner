package com.softcg.myapplication.ui.Inicio.Fragments.Agenda

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Adapters.AgendaAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Adapters.UnifiedAgendaAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AgendaFragment : Fragment() {

    private val agendaViewModel: AgendaViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_diary, container, false)
        initRecyclerAgenda(view)
        return view
    }

    fun initRecyclerAgenda(view: View){
        agendaViewModel.obtenerTareas()
        agendaViewModel.obtenerEventos()
        agendaViewModel.obtenerAgendaList()
        val adapter = UnifiedAgendaAdapter(requireContext(), agendaViewModel)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycleragenda)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        agendaViewModel._listAgenda.observe(viewLifecycleOwner, Observer { AgendaItem ->
            adapter.setData(AgendaItem)
            if (AgendaItem.isNotEmpty()) {
                view.findViewById<TextView>(R.id.textoAgenda).visibility = View.GONE
            } else{
                view.findViewById<TextView>(R.id.textoAgenda).visibility = View.VISIBLE
            }
        })
    }


}