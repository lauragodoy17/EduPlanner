package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.ex.showDatePicker

class TareaDialog(
    private val context: Context,
    private val asignaturas: List<String>,
    private val onGuardar: (titulo: String, descrip: String, asignatura: String, fecha: String, prioridad: Int) -> Unit
) {

    companion object {
        val PRIORIDADES = listOf("Alta", "Media", "Baja")

        /** Alta = 1, Media = 2, Baja = 3; 0 si no se eligió ninguna. */
        fun prioridadDesdeTexto(texto: String): Int = PRIORIDADES.indexOf(texto) + 1
    }

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_tarea)

        val titulo = dialog.findViewById<EditText>(R.id.NombreEditText)
        val descripcion = dialog.findViewById<EditText>(R.id.DescripcionEditText)
        val asignatura = dialog.findViewById<AutoCompleteTextView>(R.id.AsignaturaEditText)
        val fecha = dialog.findViewById<EditText>(R.id.FechaEditText)
        val prioridad = dialog.findViewById<AutoCompleteTextView>(R.id.PriorityEditText)
        val guardarBoton = dialog.findViewById<Button>(R.id.botonAgregar)

        asignatura.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, asignaturas))
        prioridad.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, PRIORIDADES))

        fecha.setOnClickListener {
            fecha.showDatePicker()
        }

        guardarBoton.setOnClickListener {
            dialog.dismiss()
            onGuardar(
                titulo.text.toString(),
                descripcion.text.toString(),
                asignatura.text.toString(),
                fecha.text.toString(),
                prioridadDesdeTexto(prioridad.text.toString())
            )
        }

        dialog.showAsBottomSheet()
    }
}
