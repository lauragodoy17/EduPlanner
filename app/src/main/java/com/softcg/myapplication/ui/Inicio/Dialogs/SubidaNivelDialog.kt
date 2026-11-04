package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.TextView
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.Celebracion
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views.ConfettiView

/** La celebración grande: solo para subir de nivel, que pasa pocas veces y debe sentirse. */
class SubidaNivelDialog(
    private val context: Context,
    private val subida: Celebracion.SubidaNivel,
    private val onCerrado: () -> Unit
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_subida_nivel)

        dialog.findViewById<TextView>(R.id.subidaNumero).text = subida.nivel.numero.toString()
        dialog.findViewById<TextView>(R.id.subidaNombre).text = subida.nivel.nombre
        dialog.findViewById<TextView>(R.id.subidaRecompensa).text = "+${subida.monedas} EduCoins"
        dialog.findViewById<TextView>(R.id.subidaDesbloqueos).apply {
            visibility = if (subida.desbloqueos.isEmpty()) View.GONE else View.VISIBLE
            text = "Ya disponible en la tienda: " + subida.desbloqueos.joinToString { it.nombre }
        }
        dialog.findViewById<Button>(R.id.subidaContinuar).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { onCerrado() }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<ConfettiView>(R.id.subidaConfeti).lanzar()
        dialog.findViewById<View>(R.id.subidaTarjeta).apply {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            scaleX = 0.7f
            scaleY = 0.7f
            alpha = 0f
            animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(420)
                .setInterpolator(OvershootInterpolator(1.4f))
                .start()
        }
    }
}
