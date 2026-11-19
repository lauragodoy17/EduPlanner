package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion

/** Registrar una calificación o, si se pasa [calificacion], editarla. */
class CalificacionDialog(
    private val context: Context,
    private val asignaturas: List<String>,
    private val calificacion: Calificacion? = null,
    private val onGuardar: (tipo: String, valor: Float, asignatura: String, descripcion: String) -> Unit
) {

    companion object {
        val TIPOS = listOf("Numerico (1-5)")

        /** Un valor vacío o no numérico se guarda como 0. */
        fun valorDesdeTexto(texto: String): Float = texto.toFloatOrNull() ?: 0F
    }

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_calificacion)

        val tipo = dialog.findViewById<AutoCompleteTextView>(R.id.CalificacionEditText)
        val asignatura = dialog.findViewById<AutoCompleteTextView>(R.id.AsignaturaEditText)
        val campoValor = dialog.findViewById<EditText>(R.id.NombreEditText)
        val descripcion = dialog.findViewById<EditText>(R.id.DescripcionEditText)
        val guardarBoton = dialog.findViewById<Button>(R.id.botonAgregar)

        tipo.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, TIPOS))
        asignatura.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, asignaturas))

        this.calificacion?.let {
            dialog.findViewById<TextView>(R.id.formTitulo).text = "Editar calificación"
            guardarBoton.text = "Guardar cambios"
            tipo.setText(it.tipo, false)
            asignatura.setText(it.asignatura, false)
            campoValor.setText(if (it.valor % 1f == 0f) it.valor.toInt().toString() else it.valor.toString())
            descripcion.setText(it.descripcion)
        }

        guardarBoton.setOnClickListener {
            dialog.dismiss()
            onGuardar(
                tipo.text.toString(),
                valorDesdeTexto(campoValor.text.toString()),
                asignatura.text.toString(),
                descripcion.text.toString()
            )
        }

        dialog.showAsBottomSheet()
    }
}
