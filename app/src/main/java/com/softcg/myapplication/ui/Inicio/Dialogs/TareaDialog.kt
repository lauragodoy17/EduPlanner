package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import com.softcg.myapplication.domain.TextoUsuario
import android.content.Context
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.ex.showDatePicker
import com.softcg.myapplication.core.ex.showTimePicker
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import java.time.LocalDate

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

        /** La fecha del campo (yyyy-MM-dd); null si está vacía o no es una fecha. */
        fun fechaValida(texto: String): LocalDate? = runCatching { LocalDate.parse(texto.trim()) }.getOrNull()

        /** Tareas y eventos van como mucho a este número de años: más lejos casi siempre es un error al elegir. */
        const val MAX_ANIOS_ADELANTE = 2L

        /** null si la fecha sirve; si no, el error para el campo. Las pasadas se aceptan (registrar algo atrasado). */
        fun errorFecha(texto: String, hoy: LocalDate = LocalDate.now()): String? {
            val fecha = fechaValida(texto) ?: return "Elige la fecha"
            return if (fecha.isAfter(hoy.plusYears(MAX_ANIOS_ADELANTE))) "Elige una fecha dentro de los próximos $MAX_ANIOS_ADELANTE años" else null
        }

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
        val tituloContainer = dialog.findViewById<TextInputLayout>(R.id.TituloContainer)
        val fechaContainer = dialog.findViewById<TextInputLayout>(R.id.FechaContainer)

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
        // Una tarea nueva empieza en "Media": la prioridad es obligatoria y casi siempre es esa
        prioridad.setText(textoPrioridad(tarea?.prioridad?.takeIf { it in 1..3 } ?: 2), false)

        fecha.setOnClickListener {
            fechaContainer.error = null
            fecha.showDatePicker(maxima = LocalDate.now().plusYears(MAX_ANIOS_ADELANTE))
        }
        titulo.doAfterTextChanged { if (!it.isNullOrBlank()) tituloContainer.error = null }
        horaEntrega.setOnClickListener { horaEntrega.showTimePicker() }
        // El icono del reloj abre el selector; con una hora elegida, mantener pulsado el campo la quita
        horaContainer.setEndIconOnClickListener { horaEntrega.showTimePicker() }
        horaEntrega.setOnLongClickListener { horaEntrega.setText(""); true }

        guardarBoton.setOnClickListener {
            val elegida = asignatura.text.toString()
            // Sin título ni fecha la tarea no se puede mostrar en la agenda ni avisar a tiempo: se piden aquí
            val textoTitulo = TextoUsuario.linea(titulo.text.toString(), TextoUsuario.MAX_TITULO)
            val sinTitulo = textoTitulo.isEmpty()
            val errorFecha = errorFecha(fecha.text.toString())
            val sinPrioridad = prioridadDesdeTexto(prioridad.text.toString()) == 0
            tituloContainer.error = if (sinTitulo) "Escribe el título de la tarea" else null
            fechaContainer.error = errorFecha?.let { if (fechaValida(fecha.text.toString()) == null) "Elige la fecha de entrega" else it }
            asignaturaContainer.error = if (!asignaturaValida(elegida, asignaturas)) context.getString(R.string.tarea_error_asignatura) else null
            dialog.findViewById<TextInputLayout>(R.id.PrioridadContainer)?.error = if (sinPrioridad) "Elige la prioridad" else null
            if (sinTitulo || errorFecha != null || sinPrioridad || asignaturaContainer.error != null) return@setOnClickListener
            dialog.dismiss()
            onGuardar(
                textoTitulo,
                TextoUsuario.parrafo(descripcion.text.toString(), TextoUsuario.MAX_DESCRIPCION),
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
