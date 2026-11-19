package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.ex.showTimePicker
import com.softcg.myapplication.domain.CrearAsignaturaUseCase
import com.softcg.myapplication.domain.NuevaAsignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Adapters.MultiSelectSpinnerAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.ListItem

/**
 * Crear una asignatura o, si se pasa [asignatura], editarla. Se abre desde Asignaturas, desde el formulario de tarea
 * y desde la pantalla de la asignatura. [onCerrar] se llama siempre al cerrarse; `guardada` dice si se pulsó guardar.
 */
class AsignaturaDialog(
    private val context: Context,
    private val asignatura: Asignatura? = null,
    private val onGuardar: (NuevaAsignatura) -> Unit,
    private val onCerrar: (guardada: Boolean) -> Unit = {}
) {

    companion object {
        private const val SIN_TIPO = "Selecciona un tipo"
        private const val OTRO = "Otro"
        val TIPOS_CALIFICACION = listOf(
            SIN_TIPO, "Numérico (0-10)", "Porcentaje (0-100%)", "Letras (A-F)", "Aprobado/Reprobado", OTRO
        )

        /** Texto del tipo de calificación que se guarda: el escrito a mano con "Otro", vacío sin elegir. */
        fun tipoCalificacion(seleccionado: String, otro: String): String = when (seleccionado) {
            OTRO -> otro.trim()
            SIN_TIPO -> ""
            else -> seleccionado
        }

        /** Posición del tipo guardado en el desplegable: uno escrito a mano es "Otro"; vacío, sin elegir. */
        fun posicionTipo(tipo: String): Int = when {
            tipo.isBlank() -> 0
            tipo in TIPOS_CALIFICACION -> TIPOS_CALIFICACION.indexOf(tipo)
            else -> TIPOS_CALIFICACION.indexOf(OTRO)
        }

        /** Las horas escritas; 0 si está vacío o no es un número. */
        fun duracionDesdeTexto(texto: String): Int = texto.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_agregar_asignatura)

        val nombreContainer = dialog.findViewById<TextInputLayout>(R.id.NombreContainer)
        val nombre = dialog.findViewById<EditText>(R.id.NombreEditText)
        val tutor = dialog.findViewById<EditText>(R.id.TutorEditText)
        val spinnerDias = dialog.findViewById<Spinner>(R.id.spinner)
        val hora = dialog.findViewById<EditText>(R.id.etTime)
        val duracion = dialog.findViewById<EditText>(R.id.DuracionEditText)
        val guardarBoton = dialog.findViewById<Button>(R.id.botonAgregar)
        val spinnerTipo = dialog.findViewById<Spinner>(R.id.spinnerTipoCalificacion)
        val labelTipoOtro = dialog.findViewById<View>(R.id.labelTipoCalificacionOtro)
        val layoutTipoOtro = dialog.findViewById<View>(R.id.layoutTipoCalificacionOtro)
        val tipoOtro = dialog.findViewById<EditText>(R.id.TipoCalificacionOtroEditText)

        val dias = ArrayList(CrearAsignaturaUseCase.DIAS.map { ListItem(it) })
        val yaElegidos = asignatura?.horario?.let { horario -> dias.filterIndexed { i, _ -> horario.getOrElse(i) { false } } }.orEmpty()
        var diasElegidos: List<ListItem> = yaElegidos
        val adapterDias = MultiSelectSpinnerAdapter(context, dias, yaElegidos.toMutableList())
        adapterDias.setOnItemSelectedListener(object : MultiSelectSpinnerAdapter.OnItemSelectedListener {
            override fun onItemSelected(selectedItems: List<ListItem>, pos: Int) {
                diasElegidos = selectedItems.toList()
            }
        })
        spinnerDias.adapter = adapterDias

        spinnerTipo.adapter = ArrayAdapter(context, R.layout.item_menu_asignatura, TIPOS_CALIFICACION).apply {
            setDropDownViewResource(R.layout.item_menu_asignatura)
        }
        spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val visible = if (TIPOS_CALIFICACION[position] == OTRO) View.VISIBLE else View.GONE
                labelTipoOtro.visibility = visible
                layoutTipoOtro.visibility = visible
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        asignatura?.let {
            dialog.findViewById<TextView>(R.id.formTitulo).text = "Editar asignatura"
            guardarBoton.text = "Guardar cambios"
            nombre.setText(it.nombre)
            tutor.setText(it.tutor)
            hora.setText(it.hora)
            if (it.duracion > 0) duracion.setText(it.duracion.toString())
            spinnerTipo.setSelection(posicionTipo(it.tipoCalificacion))
            if (posicionTipo(it.tipoCalificacion) == TIPOS_CALIFICACION.indexOf(OTRO)) tipoOtro.setText(it.tipoCalificacion)
        }

        hora.setOnClickListener { hora.showTimePicker() }
        nombre.setOnFocusChangeListener { _, _ -> nombreContainer.error = null }

        var guardada = false
        guardarBoton.setOnClickListener {
            if (nombre.text.isNullOrBlank()) {
                nombreContainer.error = context.getString(R.string.asignatura_error_nombre)
                return@setOnClickListener
            }
            guardada = true
            onGuardar(
                NuevaAsignatura(
                    nombre = nombre.text.toString(),
                    tutor = tutor.text.toString(),
                    duracion = duracionDesdeTexto(duracion.text.toString()),
                    hora = hora.text.toString(),
                    dias = diasElegidos.map { it.name },
                    tipoCalificacion = tipoCalificacion(spinnerTipo.selectedItem.toString(), tipoOtro.text.toString())
                )
            )
            dialog.dismiss()
        }
        dialog.setOnDismissListener { onCerrar(guardada) }

        dialog.showAsBottomSheet()
    }
}
