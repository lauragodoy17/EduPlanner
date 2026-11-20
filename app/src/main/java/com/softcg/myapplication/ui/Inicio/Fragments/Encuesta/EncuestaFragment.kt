package com.softcg.myapplication.ui.Inicio.Fragments.Encuesta

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.domain.encuesta.EncuestaCatalogo
import com.softcg.myapplication.domain.encuesta.Pregunta
import com.softcg.myapplication.domain.encuesta.ReglasEncuesta
import com.softcg.myapplication.domain.encuesta.SeccionEncuesta
import dagger.hilt.android.AndroidEntryPoint

/**
 * Encuesta de evaluación (abierta desde Ayuda y opiniones). Cada sección es una página; las preguntas se dibujan
 * desde [EncuestaCatalogo] con chips (1 a 5, 1 a 10, 0 a 10 u opciones) o un campo de texto, sin entradas libres donde no
 * hacen falta. Al terminar, las respuestas se escriben en la hoja de Google Sheets.
 */
@AndroidEntryPoint
class EncuestaFragment : Fragment() {

    private val viewModel: EncuestaViewModel by viewModels()

    /** Tarjeta de cada pregunta de la sección que se ve, para marcar las que faltan y mostrar las condicionales. */
    private val tarjetas = linkedMapOf<String, View>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_encuesta, container, false)
        val siguiente = view.findViewById<MaterialButton>(R.id.encuestaSiguiente)
        val anterior = view.findViewById<MaterialButton>(R.id.encuestaAnterior)
        siguiente.setOnClickListener { viewModel.siguiente() }
        anterior.setOnClickListener { viewModel.anterior() }
        view.findViewById<View>(R.id.encuestaVolver).setOnClickListener { findNavController().navigateUp() }

        viewModel.seccion.observe(viewLifecycleOwner) { indice ->
            pintarSeccion(view, indice)
            view.findViewById<NestedScrollView>(R.id.encuestaScroll).scrollTo(0, 0)
        }
        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            view.findViewById<View>(R.id.encuestaGracias).visibility =
                if (estado == EstadoEncuesta.Enviada) View.VISIBLE else View.GONE
            pintarBotones(view, viewModel.seccion.value ?: 0, estado)
        }
        viewModel.faltantes.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { marcarFaltantes(view, it) }
        }
        viewModel.mensaje.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { view.mostrarAviso(it) }
        }
        return view
    }

    private fun pintarSeccion(view: View, indice: Int) {
        val seccion = EncuestaCatalogo.secciones[indice]
        val total = viewModel.totalSecciones
        view.findViewById<TextView>(R.id.encuestaPaso).text = "SECCIÓN ${indice + 1} DE $total"
        view.findViewById<LinearProgressIndicator>(R.id.encuestaProgreso).apply {
            max = total
            setProgressCompat(indice + 1, true)
        }
        view.findViewById<TextView>(R.id.encuestaTitulo).text = seccion.titulo
        view.findViewById<TextView>(R.id.encuestaDescripcion).text = seccion.descripcion

        val contenedor = view.findViewById<LinearLayout>(R.id.encuestaPreguntas)
        contenedor.removeAllViews()
        tarjetas.clear()
        seccion.preguntas.forEachIndexed { i, pregunta ->
            val tarjeta = layoutInflater.inflate(R.layout.item_pregunta_encuesta, contenedor, false)
            tarjeta.findViewById<TextView>(R.id.preguntaTexto).text =
                "${i + 1}. ${pregunta.texto}" + if (pregunta.obligatoria) "" else " (opcional)"
            when (pregunta) {
                is Pregunta.Likert -> escala(tarjeta, pregunta.id, 1..5, "1 = ${EncuestaCatalogo.ESCALA_MIN}", "5 = ${EncuestaCatalogo.ESCALA_MAX}")
                is Pregunta.Puntaje -> escala(
                    tarjeta, pregunta.id, pregunta.rango, "${pregunta.desde} = ${pregunta.minimo}", "${pregunta.hasta} = ${pregunta.maximo}"
                )
                is Pregunta.Opcion -> opciones(tarjeta, seccion, pregunta)
                is Pregunta.Texto -> texto(tarjeta, pregunta)
            }
            contenedor.addView(tarjeta)
            tarjetas[pregunta.id] = tarjeta
        }
        actualizarVisibles(seccion)
        pintarBotones(view, indice, viewModel.estado.value ?: EstadoEncuesta.Respondiendo)
    }

    private fun pintarBotones(view: View, indice: Int, estado: EstadoEncuesta) {
        val ultima = indice == viewModel.totalSecciones - 1
        view.findViewById<View>(R.id.encuestaAnterior).visibility = if (indice == 0) View.GONE else View.VISIBLE
        view.findViewById<MaterialButton>(R.id.encuestaSiguiente).apply {
            isEnabled = estado != EstadoEncuesta.Enviando
            text = when {
                estado == EstadoEncuesta.Enviando -> "Enviando…"
                ultima -> "Enviar respuestas"
                else -> "Siguiente"
            }
        }
    }

    /** Chips numerados (1 a 5 o 0 a 10) con el significado de los extremos debajo. */
    private fun escala(tarjeta: View, id: String, rango: IntRange, minimo: String, maximo: String) {
        tarjeta.findViewById<View>(R.id.preguntaExtremos).visibility = View.VISIBLE
        tarjeta.findViewById<TextView>(R.id.preguntaMinimo).text = minimo
        tarjeta.findViewById<TextView>(R.id.preguntaMaximo).text = maximo
        llenarChips(tarjeta, id, rango.map { it.toString() }) { ocultarError(tarjeta) }
    }

    private fun opciones(tarjeta: View, seccion: SeccionEncuesta, pregunta: Pregunta.Opcion) {
        val campo = tarjeta.findViewById<TextInputLayout>(R.id.preguntaCampo)
        val detalle = pregunta.conDetalle
        if (detalle != null) {
            val clave = ReglasEncuesta.claveDetalle(pregunta)
            configurarCampo(campo, pregunta.detalleHint, ReglasEncuesta.MAX_DETALLE, varias = false, clave = clave) { ocultarError(tarjeta) }
            campo.visibility = if (viewModel.respuesta(pregunta.id) == detalle) View.VISIBLE else View.GONE
        }
        llenarChips(tarjeta, pregunta.id, pregunta.opciones) { elegida ->
            ocultarError(tarjeta)
            if (detalle != null) campo.visibility = if (elegida == detalle) View.VISIBLE else View.GONE
            // Responder "No" en el QR oculta (y deja de exigir) las demás preguntas de la sección
            actualizarVisibles(seccion)
        }
    }

    private fun texto(tarjeta: View, pregunta: Pregunta.Texto) {
        val campo = tarjeta.findViewById<TextInputLayout>(R.id.preguntaCampo)
        campo.visibility = View.VISIBLE
        configurarCampo(campo, pregunta.hint.ifBlank { "Tu respuesta" }, pregunta.maximo, pregunta.varias, pregunta.id) { ocultarError(tarjeta) }
    }

    private fun configurarCampo(campo: TextInputLayout, hint: String, maximo: Int, varias: Boolean, clave: String, onCambio: () -> Unit) {
        campo.hint = hint
        campo.counterMaxLength = maximo
        val entrada = campo.findViewById<TextInputEditText>(R.id.preguntaEntrada)
        entrada.filters = arrayOf(InputFilter.LengthFilter(maximo))
        if (varias) {
            entrada.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            entrada.minLines = 3
            entrada.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        } else {
            entrada.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            entrada.maxLines = 1
        }
        entrada.setText(viewModel.respuesta(clave).orEmpty())
        entrada.doAfterTextChanged {
            viewModel.responder(clave, it?.toString()?.takeIf { texto -> texto.isNotBlank() })
            onCambio()
        }
    }

    /** Un chip por opción, con la respuesta guardada ya marcada; elegir guarda la respuesta al momento. */
    private fun llenarChips(tarjeta: View, id: String, valores: List<String>, onElegir: (String?) -> Unit) {
        val grupo = tarjeta.findViewById<ChipGroup>(R.id.preguntaOpciones)
        val guardada = viewModel.respuesta(id)
        valores.forEach { valor ->
            val chip = layoutInflater.inflate(R.layout.item_chip_opcion, grupo, false) as Chip
            chip.id = View.generateViewId()
            chip.text = valor
            chip.tag = valor
            grupo.addView(chip)
            if (valor == guardada) chip.isChecked = true
        }
        grupo.setOnCheckedStateChangeListener { g, ids ->
            val elegida = ids.firstOrNull()?.let { g.findViewById<Chip>(it)?.tag as? String }
            viewModel.responder(id, elegida)
            onElegir(elegida)
        }
    }

    private fun actualizarVisibles(seccion: SeccionEncuesta) {
        val respuestas = viewModel.respuestas()
        seccion.preguntas.forEach { pregunta ->
            tarjetas[pregunta.id]?.visibility = if (ReglasEncuesta.visible(pregunta, respuestas)) View.VISIBLE else View.GONE
        }
    }

    private fun marcarFaltantes(view: View, ids: List<String>) {
        ids.forEach { id ->
            val tarjeta = tarjetas[id] ?: return@forEach
            val pregunta = EncuestaCatalogo.pregunta(id)
            // Eligió "Otra app" pero no escribió cuál
            val faltaDetalle = pregunta is Pregunta.Opcion && pregunta.conDetalle != null &&
                viewModel.respuesta(id) == pregunta.conDetalle
            tarjeta.findViewById<TextView>(R.id.preguntaError).apply {
                text = if (faltaDetalle) "Escribe ${(pregunta as Pregunta.Opcion).detalleHint.lowercase().trim('¿', '?')} para continuar"
                else "Responde esta pregunta para continuar"
                visibility = View.VISIBLE
            }
        }
        val primera = ids.firstNotNullOfOrNull { tarjetas[it] } ?: return
        val contenedor = view.findViewById<View>(R.id.encuestaPreguntas)
        view.findViewById<NestedScrollView>(R.id.encuestaScroll).smoothScrollTo(0, contenedor.top + primera.top - 24)
        view.mostrarAviso(if (ids.size == 1) "Falta 1 pregunta por responder" else "Faltan ${ids.size} preguntas por responder")
    }

    private fun ocultarError(tarjeta: View) {
        tarjeta.findViewById<View>(R.id.preguntaError).visibility = View.GONE
    }
}
