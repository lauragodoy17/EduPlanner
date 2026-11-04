package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.gamificacion.AccesoriosCatalogo
import com.softcg.myapplication.domain.gamificacion.NivelesConfig
import com.softcg.myapplication.domain.gamificacion.ProgresoNivel

/** La escala completa de niveles: dónde está el usuario y qué desbloquea cada uno. */
class RutaNivelesDialog(
    private val context: Context,
    private val progreso: ProgresoNivel
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_ruta_niveles)

        dialog.findViewById<TextView>(R.id.rutaResumen).text = "${progreso.xp} XP acumulados. Comprar en la tienda no te baja de nivel."

        val lista = dialog.findViewById<LinearLayout>(R.id.rutaLista)
        val inflater = LayoutInflater.from(context)
        NivelesConfig.niveles.forEach { nivel ->
            val fila = inflater.inflate(R.layout.item_nivel, lista, false)
            val alcanzado = progreso.nivel.numero >= nivel.numero
            val actual = progreso.nivel.numero == nivel.numero

            fila.findViewById<TextView>(R.id.nivelNumero).apply {
                text = nivel.numero.toString()
                backgroundTintList = ColorStateList.valueOf(
                    context.getColor(if (alcanzado) R.color.purple_accent else R.color.logro_bloqueado)
                )
            }
            fila.findViewById<TextView>(R.id.nivelNombre).text = nivel.nombre + if (actual) " · Estás aquí" else ""
            fila.findViewById<TextView>(R.id.nivelXp).text = buildString {
                append("${nivel.xpDesde} XP")
                if (nivel.recompensa > 0) append(" · +${nivel.recompensa} EduCoins")
            }
            val desbloqueos = AccesoriosCatalogo.accesorios.filter { it.nivelMinimo == nivel.numero && it.nivelMinimo > 1 }
            fila.findViewById<TextView>(R.id.nivelDesbloqueos).apply {
                visibility = if (desbloqueos.isEmpty()) View.GONE else View.VISIBLE
                text = "Desbloquea: " + desbloqueos.joinToString { it.nombre }
            }
            if (actual) fila.setBackgroundResource(R.drawable.bg_pastilla_superficie)
            lista.addView(fila)
        }

        dialog.findViewById<Button>(R.id.rutaCerrar).setOnClickListener { dialog.dismiss() }
        dialog.showAsBottomSheet()
    }
}
