package com.softcg.myapplication.ui.tema

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.preferencias.TemaApp

/**
 * Hoja para elegir el tema. Cada opción pinta su muestra con los colores de su propio tema (resueltos con un
 * ContextThemeWrapper), así lo que se ve es exactamente lo que se aplicará.
 */
class TemaDialog(
    private val context: Context,
    private val temas: List<TemaApp>,
    private val actual: TemaApp,
    private val onElegir: (TemaApp) -> Unit
) {
    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_tema)

        val opciones = dialog.findViewById<LinearLayout>(R.id.temaOpciones)
        val inflater = LayoutInflater.from(context)
        temas.forEach { tema ->
            val fila = inflater.inflate(R.layout.item_tema, opciones, false)
            pintarOpcion(fila, tema, elegido = tema == actual)
            fila.setOnClickListener {
                dialog.dismiss()
                onElegir(tema)
            }
            opciones.addView(fila)
        }
        dialog.findViewById<View>(R.id.temaCerrar).setOnClickListener { dialog.dismiss() }
        dialog.showAsBottomSheet()
    }

    private fun pintarOpcion(fila: View, tema: TemaApp, elegido: Boolean) {
        fila.findViewById<TextView>(R.id.temaNombre).text = tema.nombre
        fila.findViewById<TextView>(R.id.temaDescripcion).text = tema.descripcion
        fila.findViewById<View>(R.id.temaElegido).visibility = if (elegido) View.VISIBLE else View.INVISIBLE
        fila.contentDescription = "Tema ${tema.nombre}. ${tema.descripcion}" + if (elegido) ". Elegido" else ""

        (fila as MaterialCardView).apply {
            strokeColor = context.getColor(if (elegido) R.color.acento else R.color.borde_suave)
            strokeWidth = ((if (elegido) 2 else 1) * resources.displayMetrics.density).toInt()
            setCardBackgroundColor(context.getColor(if (elegido) R.color.marca_superficie else R.color.superficie))
        }

        // Los colores de la muestra salen del tema que representa, no del activo
        val suyo = ContextThemeWrapper(context, tema.estilo)
        fila.findViewById<MaterialCardView>(R.id.temaMuestra).setCardBackgroundColor(suyo.getColor(R.color.fondo_app))
        fila.findViewById<View>(R.id.temaMuestraCabecera).background = ContextCompat.getDrawable(suyo, R.drawable.bg_cabecera_recta)
        fila.findViewById<View>(R.id.temaMuestraTarjeta).backgroundTintList = ColorStateList.valueOf(suyo.getColor(R.color.superficie))
        fila.findViewById<View>(R.id.temaMuestraAcento).backgroundTintList = ColorStateList.valueOf(suyo.getColor(R.color.acento))
    }
}
