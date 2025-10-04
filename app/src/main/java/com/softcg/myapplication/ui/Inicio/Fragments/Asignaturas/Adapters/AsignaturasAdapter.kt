package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.AsignaturasViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.AsignaturaConHora

class AsignaturasAdapter (private val context: Context, private val inicioViewModel: AsignaturasViewModel) : RecyclerView.Adapter<AsignaturasAdapter.MyViewHolder>()  {

    private var asignaturas = emptyList<Asignatura>()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val textNombre : TextView = itemView.findViewById(R.id.text_titulo)
        val textTutor : TextView = itemView.findViewById(R.id.textodesc)
        val boton: ImageView = itemView.findViewById(R.id.Opciones)
        val typeBadge: TextView = itemView.findViewById(R.id.item_type_badge)
        val subjectIcon: ImageView = itemView.findViewById(R.id.circleImage)
        val mainCard: CardView = itemView.findViewById(R.id.main_card)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_asignatura_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return asignaturas.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem=asignaturas[position]
        holder.textNombre.text = currentItem.nombre
        holder.textTutor.text = currentItem.tutor

        // Set type badge with dynamic color
        holder.typeBadge.text = "ASIGNATURA"

        // Apply pastel color to card background
        val colorPastel = AsignaturaConHora.getColorPastel(position)
        holder.mainCard.setCardBackgroundColor(Color.parseColor(colorPastel))

        // Adjust badge color based on card color
        val badgeColor = when (position % 4) {
            0 -> "#66BB6A" // Verde
            1 -> "#42A5F5" // Azul
            2 -> "#AB47BC" // Púrpura
            else -> "#EF5350" // Rosa
        }
        holder.typeBadge.setBackgroundColor(Color.parseColor(badgeColor))

        holder.boton.setOnClickListener {v ->
            showPopupMenu(v,position)
        }

        // Add animation for better UX
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())
            .start()
    }

    private fun showPopupMenu(view: View,position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menudos)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.Borrar -> {
                    deleteItem(position)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    fun setData(item: List<Asignatura>){
        this.asignaturas=item
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int){
        val builder = AlertDialog.Builder(context)
        val currentItem= asignaturas[position]
        builder.setPositiveButton("Si"){ _, _ ->
            inicioViewModel.deleteAsignatura(currentItem)
            Toast.makeText(context,"Asignatura Borrada ;D", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No"){ _, _ ->}
        builder.setTitle("Borrar Asignatura?")
        builder.setMessage("Esta seguro que desea borrar esta Asignatura?")
        builder.create().show()
        }

}