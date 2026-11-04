package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.LogroEstado
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Models.LogroRecursos

class LogrosAdapter(
    private val onClick: (LogroEstado) -> Unit
) : RecyclerView.Adapter<LogrosAdapter.LogroViewHolder>() {

    companion object {
        const val COLUMNAS = 3
    }

    private var logros = emptyList<LogroEstado>()

    /** Primero los conseguidos; después los que están más cerca de conseguirse. */
    fun setData(logros: List<LogroEstado>) {
        this.logros = logros.sortedWith(
            compareByDescending<LogroEstado> { it.desbloqueado }.thenByDescending { it.fraccion }
        )
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LogroViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_logro, parent, false)
    )

    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) = holder.bind(logros[position])

    override fun getItemCount() = logros.size

    inner class LogroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val insignia: ImageView = itemView.findViewById(R.id.logroInsignia)
        private val icono: ImageView = itemView.findViewById(R.id.logroIcono)
        private val nombre: TextView = itemView.findViewById(R.id.logroNombre)
        private val barra: LinearProgressIndicator = itemView.findViewById(R.id.logroBarra)

        fun bind(estado: LogroEstado) {
            val context = itemView.context
            val color = if (estado.desbloqueado) LogroRecursos.color(estado.logro.rareza) else R.color.logro_bloqueado
            insignia.imageTintList = ColorStateList.valueOf(context.getColor(color))
            icono.setImageResource(LogroRecursos.icono(estado.logro.metrica))
            nombre.text = estado.logro.titulo
            nombre.setTextColor(context.getColor(if (estado.desbloqueado) R.color.text_primary else R.color.text_secondary))

            barra.visibility = if (estado.desbloqueado) View.INVISIBLE else View.VISIBLE
            barra.max = estado.logro.meta
            barra.setProgressCompat(estado.avance, false)

            itemView.contentDescription = "${estado.logro.titulo}, ${estado.logro.rareza.nombre}, " +
                if (estado.desbloqueado) "conseguido" else "${estado.avance} de ${estado.logro.meta}"
            itemView.setOnClickListener { onClick(estado) }
        }
    }
}
