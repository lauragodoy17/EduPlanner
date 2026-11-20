package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import androidx.core.widget.doAfterTextChanged
import com.softcg.myapplication.domain.TextoUsuario
import android.content.Context
import android.view.View
import android.view.Window
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
import com.softcg.myapplication.domain.compartir.ImportarCompartidoUseCase
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Adapters.MultiSelectSpinnerAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.ListItem

/**
 * Crear una asignatura o, si se pasa [asignatura], editarla. Se abre desde Asignaturas, desde el formulario de tarea
 * y desde la pantalla de la asignatura. [onCerrar] se llama siempre al cerrarse; `guardada` dice si se pulsó guardar.
 * La escala de notas es obligatoria al crearla; al editar, [escalaBloqueada] (ya tiene notas) la deja fija.
 */
class AsignaturaDialog(
    private val context: Context,
    private val asignatura: Asignatura? = null,
    private val escalaBloqueada: Boolean = false,
    /** Nombres de las demás asignaturas: no se puede repetir uno (sin distinguir tildes ni mayúsculas). */
    private val otrosNombres: List<String> = emptyList(),
    private val onGuardar: (NuevaAsignatura) -> Unit,
    private val onCerrar: (guardada: Boolean) -> Unit = {}
) {

    companion object {
        /** Una clase dura como mucho esto: más largo dejaría la clase terminando otro día y rompería la asistencia. */
        const val MAX_DURACION = 12

        /** Las horas escritas; 0 si está vacío o no es un número. */
        fun duracionDesdeTexto(texto: String): Int = texto.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0

        /** null si la duración sirve; si no, el error para el campo. Es obligatoria: sin ella no hay horario. */
        fun errorDuracion(texto: String): String? {
            if (texto.isBlank()) return "¿Cuántas horas dura?"
            val horas = texto.trim().toIntOrNull()
            return if (horas == null || horas !in 1..MAX_DURACION) "Entre 1 y $MAX_DURACION horas" else null
        }
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
        val selectorEscala = SelectorEscala(dialog.findViewById(R.id.escalaSelector))
        selectorEscala.mostrar(asignatura?.escala, escalaBloqueada)

        val dias = ArrayList(CrearAsignaturaUseCase.DIAS.map { ListItem(it) })
        val yaElegidos = asignatura?.horario?.let { horario -> dias.filterIndexed { i, _ -> horario.getOrElse(i) { false } } }.orEmpty()
        var diasElegidos: List<ListItem> = yaElegidos
        val adapterDias = MultiSelectSpinnerAdapter(context, dias, yaElegidos.toMutableList())
        spinnerDias.adapter = adapterDias

        asignatura?.let {
            dialog.findViewById<TextView>(R.id.formTitulo).text = "Editar asignatura"
            guardarBoton.text = "Guardar cambios"
            nombre.setText(it.nombre)
            tutor.setText(it.tutor)
            hora.setText(it.hora)
            if (it.duracion > 0) duracion.setText(it.duracion.toString())
        }

        hora.setOnClickListener { hora.showTimePicker() }
        nombre.setOnFocusChangeListener { _, _ -> nombreContainer.error = null }
        val horaContainer = dialog.findViewById<TextInputLayout>(R.id.HoraContainer)
        val errorDias = dialog.findViewById<TextView>(R.id.DiasError)
        hora.doAfterTextChanged { horaContainer.error = null }
        adapterDias.setOnItemSelectedListener(object : MultiSelectSpinnerAdapter.OnItemSelectedListener {
            override fun onItemSelected(selectedItems: List<ListItem>, pos: Int) {
                diasElegidos = selectedItems.toList()
                if (diasElegidos.isNotEmpty()) errorDias.visibility = View.GONE
            }
        })

        var guardada = false
        guardarBoton.setOnClickListener {
            val textoNombre = TextoUsuario.linea(nombre.text.toString(), TextoUsuario.MAX_NOMBRE_ASIGNATURA)
            val repetido = ImportarCompartidoUseCase.buscar(textoNombre, otrosNombres)
            val sinNombre = textoNombre.isEmpty() || repetido != null
            nombreContainer.error = when {
                textoNombre.isEmpty() -> context.getString(R.string.asignatura_error_nombre)
                repetido != null -> "Ya tienes una asignatura llamada «$repetido»"
                else -> null
            }
            val errorDuracion = errorDuracion(duracion.text.toString())
            dialog.findViewById<TextInputLayout>(R.id.DuracionContainer).error = errorDuracion
            // Días y hora son obligatorios: sin ellos la clase no sale en el horario, ni se avisa, ni se pregunta asistencia
            val sinHora = hora.text.isNullOrBlank()
            horaContainer.error = if (sinHora) "Elige la hora" else null
            val sinDias = diasElegidos.isEmpty()
            errorDias.visibility = if (sinDias) View.VISIBLE else View.GONE
            // Bloqueada se conserva la que tiene; si no, hay que elegir una válida (también al editar una antigua sin escala)
            val escala = if (escalaBloqueada) null else selectorEscala.validar()
            if (sinNombre || errorDuracion != null || sinHora || sinDias || (!escalaBloqueada && escala == null)) return@setOnClickListener
            guardada = true
            onGuardar(
                NuevaAsignatura(
                    nombre = textoNombre,
                    tutor = TextoUsuario.linea(tutor.text.toString(), TextoUsuario.MAX_PERSONA),
                    duracion = duracionDesdeTexto(duracion.text.toString()),
                    hora = hora.text.toString(),
                    dias = diasElegidos.map { it.name },
                    escala = escala
                )
            )
            dialog.dismiss()
        }
        dialog.setOnDismissListener { onCerrar(guardada) }

        dialog.showAsBottomSheet()
    }
}
