package com.softcg.myapplication.ui.Inicio.Dialogs

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion
import com.softcg.myapplication.domain.calificaciones.TipoEscala

/**
 * Maneja `include_selector_escala.xml` dentro de [raiz]: un chip por cada escala de la app ([TipoEscala.ELEGIBLES]; no
 * se pueden inventar otras) y debajo cómo se verán las notas. [onCambio] recibe la escala elegida (o null).
 */
class SelectorEscala(
    raiz: View,
    private val onCambio: (EscalaCalificacion?) -> Unit = {}
) {
    private val opciones = raiz.findViewById<ChipGroup>(R.id.escalaOpciones)
    private val vista = raiz.findViewById<TextView>(R.id.escalaVista)
    private val error = raiz.findViewById<TextView>(R.id.escalaError)
    private val bloqueo = raiz.findViewById<TextView>(R.id.escalaBloqueo)

    private var elegida: EscalaCalificacion? = null
    /** Escala antigua que no está entre las de la app (solo se muestra mientras siga bloqueada). */
    private var antigua: EscalaCalificacion? = null

    init {
        val inflater = LayoutInflater.from(raiz.context)
        TipoEscala.ELEGIBLES.forEach { tipo ->
            val chip = inflater.inflate(R.layout.item_chip_opcion, opciones, false) as Chip
            chip.id = View.generateViewId()
            chip.text = tipo.corto
            chip.tag = tipo
            opciones.addView(chip)
        }
        opciones.setOnCheckedStateChangeListener { grupo, ids ->
            val tipo = ids.firstOrNull()?.let { grupo.findViewById<Chip>(it)?.tag as? TipoEscala }
            elegida = tipo?.let(EscalaCalificacion::de)
            error.visibility = View.GONE
            actualizar()
        }
        actualizar()
    }

    /** Marca la escala que ya tiene la asignatura; con [bloqueada] no se puede cambiar. */
    fun mostrar(actual: EscalaCalificacion?, bloqueada: Boolean = false) {
        val bloqueo = bloqueada && actual != null
        antigua = actual?.takeIf { it.tipo == TipoEscala.PERSONALIZADA }
        (0 until opciones.childCount).map { opciones.getChildAt(it) as Chip }
            .firstOrNull { actual != null && it.tag == actual.tipo }?.let { opciones.check(it.id) }
        // Una antigua sin notas puede pasar a una de las escalas de la app; con notas se queda como está
        if (antigua != null && bloqueo) elegida = antigua
        (0 until opciones.childCount).forEach { opciones.getChildAt(it).isEnabled = !bloqueo }
        this.bloqueo.visibility = if (bloqueo) View.VISIBLE else View.GONE
        actualizar()
    }

    /** La escala elegida; si falta, null y el error queda a la vista. */
    fun validar(): EscalaCalificacion? {
        val escala = elegida
        error.text = "Elige cómo te califican en esta asignatura"
        error.visibility = if (escala == null) View.VISIBLE else View.GONE
        return escala
    }

    private fun actualizar() {
        val escala = elegida
        vista.text = when {
            escala != null && escala == antigua -> "Escala actual: ${escala.descripcion}"
            escala != null -> escala.descripcion
            antigua != null -> "Tu escala actual (${antigua!!.descripcion.replaceFirstChar { it.lowercase() }}) ya no se usa: elige una de estas."
            else -> "Se usará para registrar tus notas de esta asignatura. Después de la primera nota no se puede cambiar."
        }
        onCambio(escala)
    }
}
