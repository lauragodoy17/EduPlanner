package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion
import com.softcg.myapplication.domain.gamificacion.CalificarTareaUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion

/**
 * Registrar una calificación o, si se pasa [calificacion], editarla. Primero se elige la asignatura y la nota se
 * introduce con su escala ([EntradaCalificacion]): no se puede guardar un valor fuera de ella. Si la asignatura es
 * antigua y aún no tiene escala, se elige aquí una sola vez y se guarda con [onEscalaNueva].
 */
class CalificacionDialog(
    private val context: Context,
    private val asignaturas: List<Asignatura>,
    private val calificacion: Calificacion? = null,
    private val onEscalaNueva: (asignaturaId: Int, escala: EscalaCalificacion) -> Unit = { _, _ -> },
    private val onGuardar: (tipo: String, valor: Float, asignatura: String, descripcion: String) -> Unit
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_calificacion)

        val campoAsignatura = dialog.findViewById<AutoCompleteTextView>(R.id.AsignaturaEditText)
        val cajaAsignatura = dialog.findViewById<TextInputLayout>(R.id.AsignaturaContainer)
        val sinAsignatura = dialog.findViewById<View>(R.id.calificacionSinAsignatura)
        val pendiente = dialog.findViewById<View>(R.id.calificacionEscalaPendiente)
        val descripcion = dialog.findViewById<EditText>(R.id.DescripcionEditText)
        val guardarBoton = dialog.findViewById<Button>(R.id.botonAgregar)

        val entrada = EntradaCalificacion(dialog.window!!.decorView)
        var asignatura: Asignatura? = null
        val selector = SelectorEscala(pendiente) { elegida -> if (asignatura?.escala == null) entrada.mostrar(elegida) }
        dialog.findViewById<TextView>(R.id.escalaRotulo).text =
            "Esta asignatura aún no tiene escala de notas. Elígela una vez:"

        fun elegirAsignatura(elegida: Asignatura, valor: Float?) {
            asignatura = elegida
            cajaAsignatura.error = null
            sinAsignatura.visibility = View.GONE
            val escala = elegida.escala
            pendiente.visibility = if (escala == null) View.VISIBLE else View.GONE
            entrada.mostrar(escala, valor)
        }

        campoAsignatura.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, asignaturas.map { it.nombre }))
        campoAsignatura.setOnItemClickListener { _, _, posicion, _ ->
            // Al cambiar de asignatura la nota empieza de nuevo en su escala
            elegirAsignatura(asignaturas[posicion], null)
        }

        calificacion?.let { nota ->
            dialog.findViewById<TextView>(R.id.formTitulo).text = "Editar calificación"
            guardarBoton.text = "Guardar cambios"
            descripcion.setText(nota.descripcion)
            val actual = asignaturas.firstOrNull { it.id == nota.asignaturaId } ?: asignaturas.firstOrNull { it.nombre == nota.asignatura }
            actual?.let {
                campoAsignatura.setText(it.nombre, false)
                elegirAsignatura(it, nota.valor)
            }
        }

        guardarBoton.setOnClickListener {
            val elegida = asignatura
            if (elegida == null) {
                cajaAsignatura.error = "Elige la asignatura"
                return@setOnClickListener
            }
            val escala = elegida.escala ?: selector.validar() ?: return@setOnClickListener
            val valor = entrada.valor()
            if (valor == null) {
                entrada.marcarFalta()
                return@setOnClickListener
            }
            if (elegida.escala == null) elegida.id?.let { onEscalaNueva(it, escala) }
            // La nota de una tarea conserva su tipo; las demás guardan su escala ("Numérico (1-5)")
            val tipo = calificacion?.tipo?.takeIf { it == CalificarTareaUseCase.TIPO_CALIFICACION_TAREA } ?: escala.etiquetaGuardada
            dialog.dismiss()
            onGuardar(tipo, valor, elegida.nombre, descripcion.text.toString())
        }

        dialog.showAsBottomSheet()
    }
}
