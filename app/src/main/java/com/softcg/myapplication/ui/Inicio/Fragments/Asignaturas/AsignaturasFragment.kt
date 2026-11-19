package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesDatosEstudio
import com.softcg.myapplication.ui.Inicio.Dialogs.AsignaturaDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.core.ex.mostrarRecompensa
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Adapters.AsignaturasAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.TipoDetalle
import com.softcg.myapplication.ui.Inicio.Fragments.Detalle.abrirDetalle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AsignaturasFragment : Fragment() {
    private val inicioViewModel: AsignaturasViewModel by viewModels()
    private val datosEstudio: DatosEstudioViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view= inflater.inflate(R.layout.fragment_subjects, container, false)
        initButtom(view)
        initRecyclerAsignaturas(view)
        return view
    }

    fun initRecyclerAsignaturas(view: View){
        inicioViewModel.obtenerAsignaturas()
        val acciones = AccionesDatosEstudio(this, datosEstudio)
        val adapter = AsignaturasAdapter(
            requireContext(), inicioViewModel,
            onVerDatos = { asignatura -> asignatura.id?.let { acciones.abrirProgreso(it) } },
            onAbrir = { asignatura -> abrirDetalle(TipoDetalle.ASIGNATURA, asignatura.id) }
        )

        // Al crear una asignatura se piden sus datos para predicciones (área, escala, interés, método)
        inicioViewModel.asignaturaCreada.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { acciones.abrirDatosAsignatura(it) }
        }
        inicioViewModel.resumenDatos.observe(viewLifecycleOwner) { adapter.setResumenes(it) }
        datosEstudio.cambios.observe(viewLifecycleOwner) { inicioViewModel.actualizarResumenDatos() }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerasignaturas)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        inicioViewModel._asignaturas.observe(viewLifecycleOwner, Observer { Asignatura ->
            adapter.setData(Asignatura)
            val emptyState = view.findViewById<View>(R.id.textoAsignaturas)
            view.findViewById<TextView>(R.id.asignaturasResumen).text = when (Asignatura.size) {
                0 -> "Tus asignaturas"
                1 -> "1 asignatura"
                else -> "${Asignatura.size} asignaturas"
            }
            if (Asignatura.isNotEmpty()) {
                emptyState.visibility = View.GONE
            } else{
                emptyState.visibility = View.VISIBLE
            }
        })

        inicioViewModel.recompensa.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { requireActivity().mostrarRecompensa(it) }
        }
    }

    fun initButtom(view: View){
        val buttonSheet = view.findViewById<Button>(R.id.botonAgregarAsignatura)
        buttonSheet.setOnClickListener {
            showDialog()
        }
    }

    private fun showDialog() {
        AsignaturaDialog(requireContext(), otrosNombres = inicioViewModel._asignaturas.value.orEmpty().map { it.nombre }, onGuardar = { nueva ->
            inicioViewModel.onAgregarAsignaturaSelected(nueva)
            requireView().mostrarAviso("Asignatura guardada")
        }).show()
    }
}