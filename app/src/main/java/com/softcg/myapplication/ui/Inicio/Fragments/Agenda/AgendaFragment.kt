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
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Adapters.UnifiedAgendaAdapter
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesDatosEstudio
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesTarea
import com.softcg.myapplication.ui.Inicio.InicioViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.TipoDetalle
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.abrirDetalle
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.abrirTarea
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import com.softcg.myapplication.domain.ia.RecomendarTareaUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AgendaFragment : Fragment() {

    private val agendaViewModel: AgendaViewModel by activityViewModels()
    private val datosEstudio: DatosEstudioViewModel by activityViewModels()
    private val inicioViewModel: InicioViewModel by activityViewModels()
    @Inject lateinit var recomendarTarea: RecomendarTareaUseCase

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
        val accionesTarea = AccionesTarea(
            this, inicioViewModel, AccionesDatosEstudio(this, datosEstudio), recomendarTarea,
            onBorrar = { agendaViewModel.deleteTarea(it) },
            onCompletarDatos = { completa -> abrirTarea(completa.id, completa.titulo, completa.asignatura) },
            onCambio = {
                agendaViewModel.obtenerTareas()
                agendaViewModel.obtenerAgendaList()
            }
        )
        accionesTarea.observarResultados()
        val adapter = UnifiedAgendaAdapter(
            requireContext(), agendaViewModel,
            onVerMasTarea = { resumida ->
                // La fila de la agenda solo guarda lo básico: el detalle usa la tarea completa (asignatura, estado)
                accionesTarea.verDetalles(agendaViewModel._tareas.value?.firstOrNull { it.id == resumida.id } ?: resumida)
            },
            // asignatura null = evento
            onAbrir = { item ->
                if (item.asignatura == null) abrirDetalle(TipoDetalle.EVENTO, item.id)
                else abrirTarea(item.id, item.titulo, item.asignatura)
            }
        )
        datosEstudio.cambios.observe(viewLifecycleOwner) { agendaViewModel.obtenerAgendaList() }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycleragenda)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        agendaViewModel._listAgenda.observe(viewLifecycleOwner, Observer { AgendaItem ->
            adapter.setData(AgendaItem)
            view.findViewById<TextView>(R.id.agendaResumen).text =
                if (AgendaItem.size == 1) "1 pendiente" else "${AgendaItem.size} pendientes"
            view.findViewById<View>(R.id.textoAgenda).visibility = if (AgendaItem.isNotEmpty()) View.GONE else View.VISIBLE
        })
    }


}