package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.MisionEstado
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Models.LogroRecursos

class MisionesAdapter(
    private val onReclamar: (MisionEstado) -> Unit
) : RecyclerView.Adapter<MisionesAdapter.MisionViewHolder>() {

    private var misiones = emptyList<MisionEstado>()

    fun setData(misiones: List<MisionEstado>) {
        this.misiones = misiones
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MisionViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_mision, parent, false)
    )

    override fun onBindViewHolder(holder: MisionViewHolder, position: Int) = holder.bind(misiones[position])

    override fun getItemCount() = misiones.size

    inner class MisionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icono: ImageView = itemView.findViewById(R.id.misionIcono)
        private val titulo: TextView = itemView.findViewById(R.id.misionTitulo)
        private val descripcion: TextView = itemView.findViewById(R.id.misionDescripcion)
        private val barra: LinearProgressIndicator = itemView.findViewById(R.id.misionBarra)
        private val avance: TextView = itemView.findViewById(R.id.misionAvance)
        private val recompensa: View = itemView.findViewById(R.id.misionRecompensa)
        private val recompensaTexto: TextView = itemView.findViewById(R.id.misionRecompensaTexto)
        private val reclamar: MaterialButton = itemView.findViewById(R.id.misionReclamar)
        private val hecha: ImageView = itemView.findViewById(R.id.misionHecha)

        fun bind(estado: MisionEstado) {
            val mision = estado.mision
            icono.setImageResource(LogroRecursos.icono(mision.metrica))
            titulo.text = mision.titulo
            descripcion.text = mision.descripcion
            barra.max = mision.meta
            barra.setProgressCompat(estado.avance, true)
            avance.text = "${estado.avance}/${mision.meta}"
            recompensaTexto.text = "+${mision.recompensa}"

            recompensa.visibility = if (estado.completada) View.GONE else View.VISIBLE
            reclamar.visibility = if (estado.reclamable) View.VISIBLE else View.GONE
            reclamar.text = "Reclamar +${mision.recompensa}"
            hecha.visibility = if (estado.reclamada) View.VISIBLE else View.GONE
            itemView.alpha = if (estado.reclamada) 0.6f else 1f

            reclamar.setOnClickListener {
                it.isEnabled = false
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onReclamar(estado)
            }
            reclamar.isEnabled = true

            itemView.contentDescription = buildString {
                append("${mision.titulo}. ${mision.descripcion}. Avance ${estado.avance} de ${mision.meta}. ")
                append(
                    when {
                        estado.reclamada -> "Recompensa reclamada"
                        estado.reclamable -> "Recompensa lista para reclamar"
                        else -> "Recompensa: ${mision.recompensa} EduCoins"
                    }
                )
            }
        }
    }
}
