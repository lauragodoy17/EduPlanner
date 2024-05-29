package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.dialog.TimePickerFragment
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
        val adapter = AsignaturasAdapter(requireContext(),inicioViewModel)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerasignaturas)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        inicioViewModel._asignaturas.observe(viewLifecycleOwner, Observer { Asignatura ->
            adapter.setData(Asignatura)
            if (Asignatura.isNotEmpty()) {
                view.findViewById<TextView>(R.id.textoAsignaturas).visibility = View.GONE
            } else{
                view.findViewById<TextView>(R.id.textoAsignaturas).visibility = View.VISIBLE
            }
        })
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
            inicioViewModel.onAgregarAsignaturaSelected(nombre.text.toString(),tutor.text.toString(),duracion.text.toString().toInt(),currentName)
            Toast.makeText(context,"Asignatura guardada", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }
    private fun showTimePickerDialog(tiempo: EditText){
        val timePicker= TimePickerFragment{onTimeSelected(it, tiempo)}
        timePicker.show(childFragmentManager, "time")
    }
    private fun onTimeSelected(time:String, tiempo: EditText){
        tiempo.setText(" $time")
    }

}