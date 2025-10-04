package com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.AsignaturaConHora

class HorarioAdapter () : RecyclerView.Adapter<HorarioAdapter.MyViewHolder>(){

    private var asignaturaslist = emptyList<AsignaturaConHora>()

    class MyViewHolder(itemeventoView: View) :RecyclerView.ViewHolder(itemeventoView) {
        val textNombre : TextView = itemeventoView.findViewById(R.id.text_titulo)
        val textTutor : TextView = itemeventoView.findViewById(R.id.textodesc)
        val textDuracion : TextView = itemeventoView.findViewById(R.id.textofecha)
        val textUbicacion: TextView = itemeventoView.findViewById(R.id.textoubi)
        val typeBadge: TextView = itemeventoView.findViewById(R.id.item_type_badge)
        val classIcon: ImageView = itemeventoView.findViewById(R.id.circleImage)
        val timeRange: TextView = itemeventoView.findViewById(R.id.time_range)
        val mainCard: CardView = itemeventoView.findViewById(R.id.main_card)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_clase_horario, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return asignaturaslist.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = asignaturaslist[position]
        val asignatura = currentItem.asignatura

        holder.textNombre.text = asignatura.nombre
        holder.textTutor.text = asignatura.tutor
        holder.textDuracion.text = "${asignatura.duracion} horas"
        holder.textUbicacion.text = "Universidad Libre"
        holder.timeRange.text = "${currentItem.horaInicio} - ${currentItem.horaFin}"

        // Set type badge with dynamic color
        holder.typeBadge.text = "HORARIO"

        // Apply pastel color to card background
        val colorPastel = AsignaturaConHora.getColorPastel(currentItem.colorIndex)
        holder.mainCard.setCardBackgroundColor(Color.parseColor(colorPastel))

        // Adjust badge color based on card color
        val badgeColor = when (currentItem.colorIndex % 4) {
            0 -> "#FF6B6B" // Rosa
            1 -> "#4ECDC4" // Teal
            2 -> "#45B7D1" // Azul
            else -> "#96CEB4" // Verde
        }
        holder.typeBadge.setBackgroundColor(Color.parseColor(badgeColor))

        // Add animation for better UX
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())
            .start()
    }

    fun setData(asignatura: List<AsignaturaConHora>){
        this.asignaturaslist = asignatura.sortedBy { it.horaInicio }
        notifyDataSetChanged()
    }

    // Helper function to convert from old format
    fun setDataFromAsignaturas(asignaturas: List<com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura>) {
        this.asignaturaslist = asignaturas.mapIndexed { index, asignatura ->
            AsignaturaConHora(asignatura, index)
        }.sortedBy { it.horaInicio }
        notifyDataSetChanged()
    }




}