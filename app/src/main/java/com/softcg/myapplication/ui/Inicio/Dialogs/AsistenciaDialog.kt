package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.formatHora
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.gamificacion.ClaseProgramada
import com.softcg.myapplication.domain.gamificacion.TipoRecompensa
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pregunta si el usuario asistió a [clase]. Solo se cierra al responder, al posponer o si vence el plazo. */
class AsistenciaDialog(
    private val context: Context,
    val clase: ClaseProgramada,
    private val onResponder: (asistio: Boolean) -> Unit,
    private val onMasTarde: () -> Unit,
    private val onCerrado: () -> Unit
) {

    companion object {
        private val FORMATO_FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))

        fun textoHorario(clase: ClaseProgramada): String {
            val inicio = formatHora(clase.inicio.hour, clase.inicio.minute)
            val fin = formatHora(clase.fin.hour, clase.fin.minute)
            val diaSiguiente = if (clase.fin.toLocalDate() != clase.inicio.toLocalDate()) " (día siguiente)" else ""
            return "$inicio – $fin$diaSiguiente"
        }
    }

    private lateinit var dialog: Dialog
    private lateinit var botones: List<Button>

    fun show() {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_confirmar_asistencia)
        dialog.setCancelable(false)

        dialog.findViewById<TextView>(R.id.asistenciaAsignatura).text = clase.nombre
        dialog.findViewById<TextView>(R.id.asistenciaFecha).text =
            clase.fecha.format(FORMATO_FECHA).replaceFirstChar { it.uppercase() }
        dialog.findViewById<TextView>(R.id.asistenciaHorario).text = textoHorario(clase)
        dialog.findViewById<TextView>(R.id.asistenciaRecompensa).text =
            "+${TipoRecompensa.ASISTENCIA.cantidad} EduCoins si asististe"

        val asisti = dialog.findViewById<Button>(R.id.botonAsisti)
        val noAsisti = dialog.findViewById<Button>(R.id.botonNoAsisti)
        val masTarde = dialog.findViewById<Button>(R.id.botonMasTarde)
        botones = listOf(asisti, noAsisti, masTarde)

        asisti.setOnClickListener { responder(true) }
        noAsisti.setOnClickListener { responder(false) }
        masTarde.setOnClickListener {
            onMasTarde()
            cerrar()
        }

        dialog.setOnDismissListener { onCerrado() }
        dialog.showAsBottomSheet()
    }

    /** Vuelve a permitir responder (p. ej. si no se pudo guardar). */
    fun habilitar() {
        botones.forEach { it.isEnabled = true }
    }

    fun cerrar() {
        if (::dialog.isInitialized && dialog.isShowing) dialog.dismiss()
    }

    private fun responder(asistio: Boolean) {
        // Evita una segunda solicitud mientras se guarda la primera
        botones.forEach { it.isEnabled = false }
        onResponder(asistio)
    }
}
