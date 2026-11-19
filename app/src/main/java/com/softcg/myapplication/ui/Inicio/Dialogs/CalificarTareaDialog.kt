package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Models.Tarea

/**
 * Registrar la calificación de una tarea la da por completada. La nota se elige con la escala de su [asignatura];
 * si esta aún no tiene escala (asignatura antigua), se elige aquí una sola vez y se guarda con [onEscalaNueva].
 */
class CalificarTareaDialog(
    private val context: Context,
    private val tarea: Tarea,
    private val asignatura: Asignatura?,
    private val onEscalaNueva: (asignaturaId: Int, escala: EscalaCalificacion) -> Unit = { _, _ -> },
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

        val entrada = EntradaCalificacion(dialog.window!!.decorView)
        val escala = asignatura?.escala
        val pendiente = dialog.findViewById<View>(R.id.calificacionEscalaPendiente)
        val selector = SelectorEscala(pendiente) { elegida -> if (escala == null) entrada.mostrar(elegida) }
        if (escala == null) {
            pendiente.visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.escalaRotulo).text =
                "${tarea.asignatura.ifBlank { "Esta asignatura" }} aún no tiene escala de notas. Elígela una vez:"
        }
        entrada.mostrar(escala)

        dialog.findViewById<Button>(R.id.botonCalificar).setOnClickListener {
            val usada = escala ?: selector.validar() ?: return@setOnClickListener
            val valor = entrada.valor()
            if (valor == null) {
                entrada.marcarFalta()
                return@setOnClickListener
            }
            if (escala == null) asignatura?.id?.let { onEscalaNueva(it, usada) }
            dialog.dismiss()
            onGuardar(valor.toString())
        }

        dialog.showAsBottomSheet()
    }
}
