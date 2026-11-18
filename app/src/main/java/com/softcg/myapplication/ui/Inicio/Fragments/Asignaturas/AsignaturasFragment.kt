package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import com.softcg.myapplication.ui.Inicio.DatosEstudioViewModel
import com.softcg.myapplication.ui.Inicio.Dialogs.AccionesDatosEstudio
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.dialog.TimePickerFragment
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.core.ex.mostrarRecompensa
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Adapters.AsignaturasAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Adapters.MultiSelectSpinnerAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.ListItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AsignaturasFragment : Fragment() {
    private var spinner : Spinner?= null
    private var name : TextView ?= null
    private var selectedItem: MutableList<ListItem>? = mutableListOf()
    private var spinnerListItem: ArrayList<ListItem> ?= ArrayList()
    private val inicioViewModel: AsignaturasViewModel by viewModels()
    private val datosEstudio: DatosEstudioViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view= inflater.inflate(R.layout.fragment_subjects, container, false)
        // Inflate the layout for this fragment
        spinnerListItem?.add(ListItem("Lunes"))
        spinnerListItem?.add(ListItem("Martes"))
        spinnerListItem?.add(ListItem("Miércoles"))
        spinnerListItem?.add(ListItem("Jueves"))
        spinnerListItem?.add(ListItem("Viernes"))
        spinnerListItem?.add(ListItem("Sábado"))
        spinnerListItem?.add(ListItem("Domingo"))
        initButtom(view)
        initRecyclerAsignaturas(view)
        return view
    }

    fun initRecyclerAsignaturas(view: View){
        inicioViewModel.obtenerAsignaturas()
        val acciones = AccionesDatosEstudio(this, datosEstudio)
        val adapter = AsignaturasAdapter(requireContext(),inicioViewModel) { asignatura ->
            asignatura.id?.let { acciones.abrirProgreso(it) }
        }

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
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_asignatura)
        var currentName:List<ListItem> = listOf(ListItem(""))
        val nombre=dialog.findViewById<EditText>(R.id.NombreEditText)
        val tutor=dialog.findViewById<EditText>(R.id.TutorEditText)
        val spinner= dialog.findViewById<Spinner>(R.id.spinner)
        val guardarBoton= dialog.findViewById<Button>(R.id.botonAgregar)
        val tiempo=dialog.findViewById<EditText>(R.id.etTime)
        val duracion=dialog.findViewById<EditText>(R.id.DuracionEditText)
        var aux=0

        // Configuración del Spinner de Tipo de Calificación
        val spinnerTipoCalificacion = dialog.findViewById<Spinner>(R.id.spinnerTipoCalificacion)
        val layoutTipoCalificacionOtro = dialog.findViewById<View>(R.id.layoutTipoCalificacionOtro)
        val labelTipoCalificacionOtro = dialog.findViewById<TextView>(R.id.labelTipoCalificacionOtro)
        val tipoCalificacionOtroEditText = dialog.findViewById<EditText>(R.id.TipoCalificacionOtroEditText)

        val tiposCalificacion = arrayOf("Selecciona un tipo", "Numérico (0-10)", "Porcentaje (0-100%)", "Letras (A-F)", "Aprobado/Reprobado", "Otro")
        val tipoCalificacionAdapter = android.widget.ArrayAdapter(requireContext(), R.layout.item_menu_asignatura, tiposCalificacion)
        tipoCalificacionAdapter.setDropDownViewResource(R.layout.item_menu_asignatura)
        spinnerTipoCalificacion.adapter = tipoCalificacionAdapter

        // Listener para mostrar/ocultar el campo personalizado
        spinnerTipoCalificacion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (tiposCalificacion[position] == "Otro") {
                    labelTipoCalificacionOtro.visibility = View.VISIBLE
                    layoutTipoCalificacionOtro.visibility = View.VISIBLE
                } else {
                    labelTipoCalificacionOtro.visibility = View.GONE
                    layoutTipoCalificacionOtro.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        selectedItem?.clear()
        val adapter = MultiSelectSpinnerAdapter(
            requireContext(),
            spinnerListItem!!,
            selectedItem!!
        )
        spinner?.adapter = adapter

        adapter.setOnItemSelectedListener(object :
            MultiSelectSpinnerAdapter.OnItemSelectedListener {
            override fun onItemSelected(
                selectedItems: List<ListItem>,
                pos: Int,
            ) {
                if (selectedItems.isEmpty()){
                    name?.text = "mmk"
                }
                else{
                    name?.text = selectedItems.joinToString(", ") { it.name }
                }
                currentName=selectedItems
            }
        }
        )
        tiempo.setOnClickListener{showTimePickerDialog(tiempo)}

        guardarBoton.setOnClickListener {
            dialog.dismiss()
            if (duracion.text.toString()!=""){
                aux=duracion.text.toString().toInt()
            }

            // Obtener el tipo de calificación seleccionado
            val tipoCalificacionSeleccionado = spinnerTipoCalificacion.selectedItem.toString()
            val tipoCalificacionFinal = if (tipoCalificacionSeleccionado == "Otro") {
                tipoCalificacionOtroEditText.text.toString()
            } else if (tipoCalificacionSeleccionado == "Selecciona un tipo") {
                ""
            } else {
                tipoCalificacionSeleccionado
            }

            inicioViewModel.onAgregarAsignaturaSelected(nombre.text.toString(),tutor.text.toString(),aux,tiempo.text.toString().trim(),currentName,tipoCalificacionFinal)
            requireView().mostrarAviso("Asignatura guardada")
        }

        dialog.showAsBottomSheet()
    }
    private fun showTimePickerDialog(tiempo: EditText){
        val timePicker= TimePickerFragment{onTimeSelected(it, tiempo)}
        timePicker.show(childFragmentManager, "time")
    }
    private fun onTimeSelected(time:String, tiempo: EditText){
        tiempo.setText(time.trim())
    }

}