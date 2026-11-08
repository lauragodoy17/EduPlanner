package com.softcg.myapplication.core.ex

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.google.android.material.snackbar.Snackbar
import com.softcg.myapplication.R

/** Colores de una prioridad: Alta = 1, Media = 2, Baja = 3. Cualquier otro valor es "sin prioridad". */
data class EstiloPrioridad(val nombre: String, @ColorRes val color: Int)

fun estiloPrioridad(prioridad: Int): EstiloPrioridad? = when (prioridad) {
    1 -> EstiloPrioridad("Alta", R.color.estado_error)
    2 -> EstiloPrioridad("Media", R.color.estado_aviso)
    3 -> EstiloPrioridad("Baja", R.color.estado_exito)
    else -> null
}

/** Pinta una forma con su tinte: `setBackgroundColor` la convertiría en un rectángulo sin esquinas. */
fun View.tintarFondo(@ColorRes color: Int) {
    backgroundTintList = ColorStateList.valueOf(context.getColor(color))
}

fun TextView.pintarEtiqueta(texto: String, @ColorRes color: Int, @ColorRes fondo: Int) {
    text = texto
    setTextColor(context.getColor(color))
    tintarFondo(fondo)
}

/** Punto y nombre de la prioridad; el nombre evita que dependa solo del color. */
fun pintarPrioridad(prioridad: Int, punto: View, nombre: TextView) {
    val estilo = estiloPrioridad(prioridad)
    punto.visibility = if (estilo == null) View.GONE else View.VISIBLE
    nombre.visibility = punto.visibility
    if (estilo != null) {
        punto.tintarFondo(estilo.color)
        nombre.text = estilo.nombre
    }
}

/** Confirmación con el mismo estilo en toda la app, para acciones que no se pueden deshacer. */
fun Context.confirmar(titulo: String, mensaje: String, textoAccion: String, onConfirmar: () -> Unit) {
    MaterialAlertDialogBuilder(this)
        .setTitle(titulo)
        .setMessage(mensaje)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(textoAccion) { _, _ -> onConfirmar() }
        .show()
}

/** Aviso breve con el estilo de la app, encima de la barra inferior y del botón flotante. */
fun View.mostrarAviso(mensaje: String, esError: Boolean = false) {
    if (!isAttachedToWindow) return
    val aviso = Snackbar.make(this, mensaje, Snackbar.LENGTH_SHORT)
    if (esError) aviso.setBackgroundTint(context.getColor(R.color.estado_error))
    val raiz = rootView
    val ancla = listOf(R.id.botonAñadir, R.id.bottom_navigation)
        .firstNotNullOfOrNull { id -> raiz.findViewById<View>(id)?.takeIf { it.visibility == View.VISIBLE } }
    if (ancla != null) aviso.anchorView = ancla
    aviso.show()
}

/** Estado de carga de un botón: se desactiva y muestra un indicador mientras dura la operación. */
fun MaterialButton.mostrarCarga(cargando: Boolean, textoNormal: String, textoCarga: String) {
    isEnabled = !cargando
    text = if (cargando) textoCarga else textoNormal
    icon = if (cargando) {
        val spec = CircularProgressIndicatorSpec(context, null, 0, com.google.android.material.R.style.Widget_MaterialComponents_CircularProgressIndicator_ExtraSmall).apply {
            indicatorColors = intArrayOf(context.getColor(R.color.white))
        }
        IndeterminateDrawable.createCircularDrawable(context, spec)
    } else null
}
