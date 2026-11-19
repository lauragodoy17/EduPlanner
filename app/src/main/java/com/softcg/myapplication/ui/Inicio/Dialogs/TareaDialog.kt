package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.ex.showDatePicker
import com.softcg.myapplication.core.ex.showTimePicker
import com.softcg.myapplication.ui.Inicio.Models.Tarea

/**
 * Crear una tarea o, si se pasa [tarea], editarla. El tema no se pregunta: para las predicciones y la bibliografía
 * es el título. Toda tarea pertenece a una asignatura: la última opción del desplegable pide [onAgregarAsignatura],
 * el formulario se oculta (conserva lo escrito) y vuelve con [asignaturaCreada] o, si se canceló, con [volver].
 * Sin [onAgregarAsignatura] (p. ej. al editar) esa opción no aparece.
 */
class TareaDialog(
    private val context: Context,
    asignaturas: List<String>,
    private val onAgregarAsignatura: (() -> Unit)?,
    private val tarea: Tarea? = null,
    private val onGuardar: (titulo: String, descrip: String, asignatura: String, fecha: String, prioridad: Int, horaEntrega: String?) -> Unit
) {

    companion object {
        val PRIORIDADES = listOf("Alta", "Media", "Baja")

        /** Texto del desplegable para una prioridad guardada; vacío si no tiene. */
        fun textoPrioridad(prioridad: Int): String = PRIORIDADES.getOrElse(prioridad - 1) { "" }

        /** Alta = 1, Media = 2, Baja = 3; 0 si no se eligió ninguna. */
        fun prioridadDesdeTexto(texto: String): Int = PRIORIDADES.indexOf(texto) + 1

        /** Solo vale una asignatura que exista; el texto de la opción "Agregar asignatura" no cuenta. */
        fun asignaturaValida(texto: String, asignaturas: List<String>): Boolean =
            texto.isNotBlank() && texto in asignaturas
    }

    private val asignaturas = asignaturas.toMutableList()
    private val opcionAgregar = context.getString(R.string.tarea_opcion_agregar_asignatura)

    private lateinit var dialog: Dialog
    private lateinit var asignaturaContainer: TextInputLayout
    private lateinit var asignatura: AutoCompleteTextView

    fun show() {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_tarea)

        val titulo = dialog.findViewById<EditText>(R.id.NombreEditText)
        val descripcion = dialog.findViewById<EditText>(R.id.DescripcionEditText)
        asignaturaContainer = dialog.findViewById(R.id.AsignaturaContainer)
        asignatura = dialog.findViewById(R.id.AsignaturaEditText)
        val fecha = dialog.findViewById<EditText>(R.id.FechaEditText)
        val horaEntrega = dialog.findViewById<EditText>(R.id.HoraEntregaEditText)
        val horaContainer = dialog.findViewById<TextInputLayout>(R.id.HoraEntregaContainer)
        val prioridad = dialog.findViewById<AutoCompleteTextView>(R.id.PriorityEditText)
        val guardarBoton = dialog.findViewById<Button>(R.id.botonAgregar)

        tarea?.let {
            dialog.findViewById<TextView>(R.id.formTitulo).text = "Editar tarea"
            guardarBoton.text = "Guardar cambios"
            titulo.setText(it.titulo)
            descripcion.setText(it.descrip)
            fecha.setText(it.fecha)
            it.horaEntrega?.let(horaEntrega::setText)
            // Una asignatura que ya no existe no se puede volver a guardar: se pide otra
            if (it.asignatura in asignaturas) asignatura.setText(it.asignatura, false)
        }

        actualizarOpcionesAsignatura()
        asignatura.setOnItemClickListener { parent, _, position, _ ->
            if (parent.getItemAtPosition(position) == opcionAgregar) {
                asignatura.setText("", false)
                dialog.hide()
                onAgregarAsignatura?.invoke()
            } else {
                asignaturaContainer.error = null
            }
        }
        prioridad.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, PRIORIDADES))
        // setText con filtro dejaría el desplegable mostrando solo la opción escrita
        if (tarea != null) prioridad.setText(textoPrioridad(tarea.prioridad), false)

        fecha.setOnClickListener {
            fecha.showDatePicker()
        }
        horaEntrega.setOnClickListener { horaEntrega.showTimePicker() }
        // El icono del reloj abre el selector; con una hora elegida, mantener pulsado el campo la quita
        horaContainer.setEndIconOnClickListener { horaEntrega.showTimePicker() }
        horaEntrega.setOnLongClickListener { horaEntrega.setText(""); true }

        guardarBoton.setOnClickListener {
            val elegida = asignatura.text.toString()
            if (!asignaturaValida(elegida, asignaturas)) {
                asignaturaContainer.error = context.getString(R.string.tarea_error_asignatura)
                return@setOnClickListener
            }
            dialog.dismiss()
            onGuardar(
                titulo.text.toString(),
                descripcion.text.toString(),
                elegida,
                fecha.text.toString(),
                prioridadDesdeTexto(prioridad.text.toString()),
                horaEntrega.text.toString().ifBlank { null }
            )
        }

        dialog.showAsBottomSheet()
    }

    /** La asignatura recién creada se agrega al desplegable, queda elegida y el formulario vuelve a mostrarse. */
    fun asignaturaCreada(nombre: String) {
        if (nombre !in asignaturas) asignaturas += nombre
        actualizarOpcionesAsignatura()
        asignatura.setText(nombre, false)
        asignaturaContainer.error = null
        volver()
    }

    /** Vuelve a mostrar el formulario tal como estaba (p. ej. si se canceló la nueva asignatura). */
    fun volver() {
        if (::dialog.isInitialized) dialog.show()
    }

    /** Cierra el formulario aunque esté oculto: si no, la ventana queda colgando al destruirse la Activity. */
    fun cerrar() {
        if (::dialog.isInitialized) dialog.dismiss()
    }

    private fun actualizarOpcionesAsignatura() {
        val opciones = if (onAgregarAsignatura != null) asignaturas + opcionAgregar else asignaturas
        asignatura.setAdapter(ArrayAdapter(context, R.layout.item_menu_asignatura, opciones))
    }
}
