package com.softcg.myapplication.ui.Inicio.Fragments.Ayuda

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.softcg.myapplication.domain.TextoUsuario
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import com.softcg.myapplication.R
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.domain.encuesta.AlmacenEncuesta
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class HelpFragment : Fragment() {

    @Inject lateinit var encuesta: AlmacenEncuesta


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_help, container, false)
        initQueja(view)
        view.findViewById<View>(R.id.botonEncuesta).setOnClickListener {
            findNavController().navigate(R.id.action_ayuda_a_encuesta)
        }
        return view
    }

    /** Al volver de la encuesta: si ya se envió, el botón solo agradece; si hay borrador, ofrece continuar. */
    override fun onResume() {
        super.onResume()
        val vista = view ?: return
        val boton = vista.findViewById<MaterialButton>(R.id.botonEncuesta)
        val detalle = vista.findViewById<TextView>(R.id.encuestaAyudaDetalle)
        when {
            encuesta.enviada() -> {
                boton.isEnabled = false
                boton.text = "¡Gracias! Ya respondiste la encuesta"
                detalle.text = "Tus respuestas ya llegaron. Nos ayudan a mejorar EduPlanner."
            }
            encuesta.borrador().isNotEmpty() -> boton.text = "Continuar la encuesta"
            else -> boton.text = "Ayúdanos con la siguiente encuesta"
        }
    }
    /** Tipo y problema se eligen de la lista; la descripción es obligatoria para que la queja sirva de algo. */
    private fun initQueja(view: View) {
        val tipo = view.findViewById<AutoCompleteTextView>(R.id.QuejaEditText)
        val problema = view.findViewById<AutoCompleteTextView>(R.id.QuejadosEditText)
        val descripcion = view.findViewById<EditText>(R.id.DescripcionEditText)
        val cajaTipo = view.findViewById<TextInputLayout>(R.id.QuejaContainer)
        val cajaProblema = view.findViewById<TextInputLayout>(R.id.QuejadosContainer)
        val cajaDescripcion = view.findViewById<TextInputLayout>(R.id.DescripcionContainer)
        tipo.setAdapter(ArrayAdapter(requireContext(), R.layout.item_menu_asignatura, TIPOS))
        problema.setAdapter(ArrayAdapter(requireContext(), R.layout.item_menu_asignatura, PROBLEMAS))
        tipo.setOnItemClickListener { _, _, _, _ -> cajaTipo.error = null }
        problema.setOnItemClickListener { _, _, _, _ -> cajaProblema.error = null }
        descripcion.doAfterTextChanged { cajaDescripcion.error = null }

        view.findViewById<Button>(R.id.botonAgregar).setOnClickListener {
            val texto = TextoUsuario.parrafo(descripcion.text.toString(), TextoUsuario.MAX_DESCRIPCION)
            cajaTipo.error = if (tipo.text.toString() in TIPOS) null else "Elige el tipo de queja"
            cajaProblema.error = if (problema.text.toString() in PROBLEMAS) null else "Elige el problema"
            cajaDescripcion.error = if (texto.length >= MIN_DESCRIPCION) null else "Cuéntanos qué pasó (al menos $MIN_DESCRIPCION caracteres)"
            if (cajaTipo.error != null || cajaProblema.error != null || cajaDescripcion.error != null) return@setOnClickListener
            tipo.setText("", false)
            problema.setText("", false)
            descripcion.setText("")
            cajaDescripcion.error = null
            requireView().mostrarAviso("Comentario enviado")
        }
    }

    private companion object {
        const val MIN_DESCRIPCION = 10
        val TIPOS = listOf("Problemas técnicos", "Problemas de usabilidad", "Problemas de contenido", "Problemas de comunicación", "Problemas de seguridad", "Problemas administrativos")
        val PROBLEMAS = listOf("La aplicación no se carga", "Funcionalidades que no funcionan", "Problemas con la interfaz de usuario", "La aplicación es lenta o se bloquea", "Alto consumo de batería", "Interfaz confusa o difícil de entender", "Problemas para encontrar ciertas funcionalidades", "Dificultad para personalizar horarios o eventos", "Problemas al agregar, editar o eliminar eventos/tareas", "Notificaciones de eventos/tareas que no funcionan correctamente", "Problemas al visualizar el calendario semanal", "Confusión con zonas horarias y fechas", "No recibir notificaciones importantes", "Problemas al iniciar sesión o recuperar contraseñas", "Dificultad para crear o eliminar cuentas de usuario")
    }
}
