package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.gamificacion.CalificarTareaUseCase
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.ui.Inicio.Models.Tarea

/** Registrar la calificación de una tarea la da por completada. */
class CalificarTareaDialog(
    private val context: Context,
    private val tarea: Tarea,
    private val onGuardar: (textoValor: String) -> Unit
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_calificar_tarea)

        dialog.findViewById<TextView>(R.id.calificarTitulo).text = tarea.titulo
        dialog.findViewById<TextView>(R.id.calificarAsignatura).text = tarea.asignatura
        dialog.findViewById<TextView>(R.id.calificarRecompensa).text =
            "Al registrar la calificación la tarea queda completada (+${TipoRecompensa.TAREA_COMPLETADA.cantidad} EduCoins)"

        val contenedor = dialog.findViewById<TextInputLayout>(R.id.calificarValorContainer)
        val valor = dialog.findViewById<EditText>(R.id.calificarValor)

        dialog.findViewById<Button>(R.id.botonCalificar).setOnClickListener {
            val texto = valor.text.toString()
            if (CalificarTareaUseCase.valorDesdeTexto(texto) == null) {
                contenedor.error = "Ingresa una calificación válida"
                return@setOnClickListener
            }
            dialog.dismiss()
            onGuardar(texto)
        }

        dialog.showAsBottomSheet()
    }
}
