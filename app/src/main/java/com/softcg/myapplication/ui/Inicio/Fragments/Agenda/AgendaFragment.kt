package com.softcg.myapplication.ui.Inicio.Fragments.Agenda

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Adapters.UnifiedAgendaAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.FiltroAgenda
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesDatosEstudio
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesTarea
import com.softcg.myapplication.ui.Inicio.InicioViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.TipoDetalle
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.abrirDetalle
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.abrirTarea
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AgendaFragment : Fragment() {

    private val agendaViewModel: AgendaViewModel by activityViewModels()
    private val datosEstudio: DatosEstudioViewModel by activityViewModels()
    private val inicioViewModel: InicioViewModel by activityViewModels()

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
            this, inicioViewModel, AccionesDatosEstudio(this, datosEstudio),
            onBorrar = { agendaViewModel.deleteTarea(it) },
            onCambio = {
                agendaViewModel.obtenerTareas()
                agendaViewModel.obtenerAgendaList()
            }
        )
        accionesTarea.observarResultados()
        val adapter = UnifiedAgendaAdapter(
            requireContext(), agendaViewModel,
            // "Ver más" abre la pantalla de la tarea, que lee la tarea completa
            onVerMasTarea = { abrirTarea(it.id, it.titulo, it.asignatura) },
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

        initFiltro(view)
        agendaViewModel._listAgenda.observe(viewLifecycleOwner, Observer { elementos ->
            adapter.setData(elementos)
            val filtro = agendaViewModel.filtro.value ?: FiltroAgenda.SIN_ENTREGAR
            view.findViewById<TextView>(R.id.agendaResumen).text = filtro.resumen(elementos.size)
            view.findViewById<TextView>(R.id.agendaVacioTitulo).text = filtro.vacio.first
            view.findViewById<TextView>(R.id.agendaVacioDetalle).text = filtro.vacio.second
            view.findViewById<View>(R.id.textoAgenda).visibility = if (elementos.isNotEmpty()) View.GONE else View.VISIBLE
        })
    }

    /** Filtro como el de Inicio: sin entregar (por defecto), solo tareas, solo eventos, entregados. */
    private fun initFiltro(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.agendaFiltro)
        val opciones = FiltroAgenda.entries
        spinner.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_pastilla, opciones.map { it.texto }).apply {
            setDropDownViewResource(R.layout.item_menu_asignatura)
        }
        spinner.setSelection(opciones.indexOf(agendaViewModel.filtro.value ?: FiltroAgenda.SIN_ENTREGAR), false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, vista: View?, posicion: Int, id: Long) {
                agendaViewModel.elegirFiltro(opciones[posicion])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}