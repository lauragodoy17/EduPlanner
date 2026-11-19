package com.softcg.myapplication.ui.Inicio.Dialogs

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.calificaciones.EscalaCalificacion

/**
 * Maneja `include_entrada_calificacion.xml` dentro de [raiz]: la nota se elige con un deslizador (y − / + para
 * afinar) en las escalas numéricas, o con una opción en letras y aprobado/reprobado. Solo admite valores de la escala.
 */
class EntradaCalificacion(raiz: View) {

    private val contenedor = raiz.findViewById<View>(R.id.notaEntrada)
    private val valorTexto = raiz.findViewById<TextView>(R.id.notaValor)
    private val escalaTexto = raiz.findViewById<TextView>(R.id.notaEscala)
    private val deslizador = raiz.findViewById<View>(R.id.notaDeslizador)
    private val extremos = raiz.findViewById<View>(R.id.notaExtremos)
    private val slider = raiz.findViewById<Slider>(R.id.notaSlider)
    private val menos = raiz.findViewById<MaterialButton>(R.id.notaMenos)
    private val mas = raiz.findViewById<MaterialButton>(R.id.notaMas)
    private val opciones = raiz.findViewById<ChipGroup>(R.id.notaOpciones)
    private val minimo = raiz.findViewById<TextView>(R.id.notaMinimo)
    private val maximo = raiz.findViewById<TextView>(R.id.notaMaximo)

    private var escala: EscalaCalificacion? = null
    private var valorElegido: Float? = null

    init {
        slider.addOnChangeListener { _, valor, desdeUsuario -> if (desdeUsuario) elegir(valor) }
        menos.setOnClickListener { mover(-1) }
        mas.setOnClickListener { mover(+1) }
        opciones.setOnCheckedStateChangeListener { grupo, ids ->
            val valor = ids.firstOrNull()?.let { grupo.findViewById<Chip>(it)?.tag as? Float }
            valorElegido = valor
            pintarValor()
        }
    }

    /** Prepara la entrada para [escala]; [valor] es la nota que ya tenía (al editar), ajustada a la escala. */
    fun mostrar(escala: EscalaCalificacion?, valor: Float? = null) {
        this.escala = escala
        contenedor.visibility = if (escala == null) View.GONE else View.VISIBLE
        escala ?: return
        escalaTexto.text = escala.descripcion

        val lista = escala.opciones
        if (lista != null) {
            deslizador.visibility = View.GONE
            extremos.visibility = View.GONE
            opciones.visibility = View.VISIBLE
            opciones.removeAllViews()
            val inflater = LayoutInflater.from(opciones.context)
            val inicial = valor?.let(escala::ajustar)
            lista.forEach { (numero, texto) ->
                val chip = inflater.inflate(R.layout.item_chip_opcion, opciones, false) as Chip
                chip.id = View.generateViewId()
                chip.text = texto
                chip.tag = numero
                opciones.addView(chip)
                if (numero == inicial) opciones.check(chip.id)
            }
            valorElegido = inicial
        } else {
            opciones.visibility = View.GONE
            deslizador.visibility = View.VISIBLE
            extremos.visibility = View.VISIBLE
            // El rango y el paso se fijan juntos: el Slider rechaza valores que no caen en un paso
            slider.valueFrom = escala.min
            slider.valueTo = escala.max
            slider.stepSize = escala.paso
            val inicial = escala.ajustar(valor ?: escala.valorInicial)
            slider.value = inicial
            minimo.text = escala.formatear(escala.min)
            maximo.text = escala.formatear(escala.max)
            valorElegido = inicial
        }
        pintarValor()
    }

    /** La nota elegida, siempre dentro de la escala; null si aún no se eligió una opción. */
    fun valor(): Float? = valorElegido

    /** Resalta que falta elegir (letras, aprobado/reprobado). */
    fun marcarFalta() {
        if (valorElegido == null) valorTexto.text = "Elige una opción"
    }

    private fun elegir(valor: Float) {
        val escala = escala ?: return
        valorElegido = escala.ajustar(valor)
        pintarValor()
    }

    private fun mover(pasos: Int) {
        val escala = escala ?: return
        val actual = valorElegido ?: escala.valorInicial
        val nuevo = escala.ajustar(actual + pasos * escala.paso)
        slider.value = nuevo
        elegir(nuevo)
    }

    private fun pintarValor() {
        val escala = escala ?: return
        val valor = valorElegido
        valorTexto.text = valor?.let(escala::formatear) ?: "—"
        valorTexto.contentDescription = valor?.let { "Calificación ${escala.formatear(it)}" } ?: "Sin calificación elegida"
        if (escala.opciones == null && valor != null) {
            menos.isEnabled = valor > escala.min
            mas.isEnabled = valor < escala.max
        }
    }
}
