package com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.AgendaViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.AgendaItem
import java.text.SimpleDateFormat
import java.util.*

class UnifiedAgendaAdapter(
    private val context: Context,
    private val agendaViewModel: AgendaViewModel
) : RecyclerView.Adapter<UnifiedAgendaAdapter.UnifiedAgendaViewHolder>() {

    private var agendaList = emptyList<AgendaItem>()

    fun setData(items: List<AgendaItem>) {
        // Sort agenda items by date
        agendaList = items.sortedBy { item ->
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.fecha)
            } catch (e: Exception) {
                Date()
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnifiedAgendaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.unified_agenda_card, parent, false)
        return UnifiedAgendaViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnifiedAgendaViewHolder, position: Int) {
        val item = agendaList[position]
        holder.bind(item)
    }

    override fun getItemCount() = agendaList.size

    inner class UnifiedAgendaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTitulo: TextView = itemView.findViewById(R.id.text_titulo)
        private val textDesc: TextView = itemView.findViewById(R.id.textodesc)
        private val textAsignatura: TextView = itemView.findViewById(R.id.texto_asignatura)
        private val textFecha: TextView = itemView.findViewById(R.id.textofecha)
        private val typeBadge: TextView = itemView.findViewById(R.id.item_type_badge)
        private val priorityIndicator: View = itemView.findViewById(R.id.priority_indicator)
        private val opciones: ImageView = itemView.findViewById(R.id.opciones)

        fun bind(item: AgendaItem) {
            textTitulo.text = item.titulo
            textDesc.text = item.descrip
            textFecha.text = formatDisplayDate(item.fecha)

            // Configure based on item type (asignatura null = evento, otherwise = tarea)
            if (item.asignatura == null) {
                // Es un evento
                textAsignatura.visibility = View.GONE
                typeBadge.text = "EVENTO"
                typeBadge.setBackgroundColor(android.graphics.Color.parseColor("#673AB7"))
            } else {
                // Es una tarea
                textAsignatura.text = item.asignatura
                textAsignatura.visibility = View.VISIBLE
                typeBadge.text = "TAREA"
                typeBadge.setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))
            }

            // Set priority indicator color based on priority level
            val priorityColor = when (item.prioridad) {
                3 -> "#4CAF50" // High priority - Green
                2 -> "#FF9800" // Medium priority - Orange
                1 -> "#F44336" // Low priority - Red
                else -> "#9C27B0" // Default purple
            }
            priorityIndicator.setBackgroundColor(android.graphics.Color.parseColor(priorityColor))

            // Set click listener for options
            opciones.setOnClickListener { view ->
                showPopupMenu(view, item)
            }

            // Add animation for better UX
            itemView.alpha = 0f
            itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay((adapterPosition * 50).toLong())
                .start()
        }

        private fun formatDisplayDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString
            }
        }

        private fun showPopupMenu(view: View, item: AgendaItem) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menudos)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.Borrar -> {
                        showDeleteDialog(item)
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }

        private fun showDeleteDialog(item: AgendaItem) {
            val builder = AlertDialog.Builder(context)

            if (item.asignatura == null) {
                // Es un evento
                builder.setPositiveButton("Sí") { _, _ ->
                    agendaViewModel.deleteEvento(item)
                    Toast.makeText(context, "Evento eliminado", Toast.LENGTH_SHORT).show()
                }
                builder.setNegativeButton("No") { _, _ -> }
                builder.setTitle("¿Eliminar Evento?")
                builder.setMessage("¿Está seguro que desea eliminar este evento?")
            } else {
                // Es una tarea
                builder.setPositiveButton("Sí") { _, _ ->
                    agendaViewModel.deleteTarea(item)
                    Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                }
                builder.setNegativeButton("No") { _, _ -> }
                builder.setTitle("¿Eliminar Tarea?")
                builder.setMessage("¿Está seguro que desea eliminar esta tarea?")
            }

            builder.create().show()
        }
    }
}