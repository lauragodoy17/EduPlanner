package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.gamificacion.AccesoriosCatalogo
import com.softcg.myapplication.domain.gamificacion.LogroEstado
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Models.LogroRecursos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Detalle de un logro: qué pide, cuánto falta y qué da. */
class LogroDialog(
    private val context: Context,
    private val estado: LogroEstado
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_logro)

        val logro = estado.logro
        val color = if (estado.desbloqueado) LogroRecursos.color(logro.rareza) else R.color.logro_bloqueado
        dialog.findViewById<ImageView>(R.id.detalleInsignia).imageTintList = ColorStateList.valueOf(context.getColor(color))
        dialog.findViewById<ImageView>(R.id.detalleIcono).setImageResource(LogroRecursos.icono(logro.metrica))
        dialog.findViewById<TextView>(R.id.detalleTitulo).text = logro.titulo
        dialog.findViewById<TextView>(R.id.detalleRareza).text = logro.rareza.nombre.uppercase()
        dialog.findViewById<TextView>(R.id.detalleDescripcion).text = logro.descripcion

        val barra = dialog.findViewById<LinearProgressIndicator>(R.id.detalleBarra)
        val avance = dialog.findViewById<TextView>(R.id.detalleAvance)
        if (estado.desbloqueado) {
            barra.visibility = View.GONE
            val fecha = SimpleDateFormat("d MMM yyyy", Locale("es")).format(Date(estado.desbloqueadoMillis ?: 0L))
            avance.text = "Conseguido el $fecha"
        } else {
            barra.max = logro.meta
            barra.progress = estado.avance
            avance.text = "${estado.avance} de ${logro.meta}"
        }

        val premio = AccesoriosCatalogo.exclusivoDeLogro(logro.id)
        dialog.findViewById<TextView>(R.id.detalleRecompensa).text = buildString {
            append("+${logro.rareza.recompensa} EduCoins")
            if (premio != null) append(" · Accesorio exclusivo: ${premio.nombre}")
        }

        dialog.findViewById<Button>(R.id.detalleCerrar).setOnClickListener { dialog.dismiss() }
        dialog.showAsBottomSheet()
    }
}
