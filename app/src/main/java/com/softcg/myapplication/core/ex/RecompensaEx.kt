package com.softcg.myapplication.core.ex

import android.app.Activity
import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.Logro
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Models.LogroRecursos

/**
 * Aviso breve de EduCoins ganados, que no tapa nada ni pide cerrarse.
 * Llamarlo solo cuando el movimiento ya se guardó.
 */
fun Activity.mostrarRecompensa(cantidad: Int, detalle: String? = null) {
    val contenedor = findViewById<ViewGroup>(android.R.id.content)
    val aviso = layoutInflater.inflate(R.layout.aviso_recompensa, contenedor, false)
    val texto = "+$cantidad EduCoins" + (detalle?.let { " · $it" } ?: "")
    aviso.findViewById<TextView>(R.id.avisoRecompensaTexto).text = texto
    contenedor.addView(aviso)

    aviso.announceForAccessibility("Ganaste $cantidad EduCoins")
    aviso.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

    val salto = 28 * resources.displayMetrics.density
    aviso.alpha = 0f
    aviso.translationY = salto
    aviso.scaleX = 0.85f
    aviso.scaleY = 0.85f
    aviso.animate()
        .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
        .setDuration(280)
        .setInterpolator(OvershootInterpolator())
        .withEndAction {
            aviso.animate()
                .alpha(0f).translationY(-salto)
                .setStartDelay(1400)
                .setDuration(320)
                .withEndAction { contenedor.removeView(aviso) }
                .start()
        }
        .start()
}

/** Avisa de un logro nuevo desde arriba. [onFin] se llama al terminar, para mostrar el siguiente de la cola. */
fun Activity.mostrarAvisoLogro(logro: Logro?, titulo: String, detalle: String, onClick: () -> Unit, onFin: () -> Unit) {
    val contenedor = findViewById<ViewGroup>(android.R.id.content)
    val aviso = layoutInflater.inflate(R.layout.aviso_logro, contenedor, false)
    aviso.findViewById<TextView>(R.id.avisoLogroTitulo).text = titulo
    aviso.findViewById<TextView>(R.id.avisoLogroDetalle).text = detalle
    if (logro != null) {
        aviso.findViewById<ImageView>(R.id.avisoLogroInsignia).imageTintList =
            ColorStateList.valueOf(getColor(LogroRecursos.color(logro.rareza)))
        aviso.findViewById<ImageView>(R.id.avisoLogroIcono).setImageResource(LogroRecursos.icono(logro.metrica))
    }
    contenedor.addView(aviso)
    aviso.announceForAccessibility("$titulo. $detalle")

    var terminado = false
    fun cerrar() {
        if (terminado) return
        terminado = true
        aviso.animate().cancel()
        aviso.animate()
            .alpha(0f).translationY(-aviso.height.toFloat())
            .setStartDelay(0)
            .setDuration(240)
            .withEndAction {
                contenedor.removeView(aviso)
                onFin()
            }
            .start()
    }
    aviso.setOnClickListener {
        onClick()
        cerrar()
    }

    aviso.visibility = View.INVISIBLE
    aviso.post {
        aviso.visibility = View.VISIBLE
        aviso.translationY = -aviso.height.toFloat()
        aviso.animate()
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(OvershootInterpolator(0.8f))
            .withEndAction { aviso.postDelayed({ cerrar() }, DURACION_AVISO_LOGRO_MS) }
            .start()
    }
}

private const val DURACION_AVISO_LOGRO_MS = 3200L

fun Activity.mostrarMensaje(mensaje: String) {
    Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show()
}
