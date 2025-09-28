package com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters

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
import com.softcg.myapplication.ui.Inicio.Models.*
import java.text.SimpleDateFormat
import java.util.*

class UnifiedItemAdapter(
    private val context: Context,
    private val onDeleteTarea: (Tarea) -> Unit = {},
    private val onDeleteEvento: (Evento) -> Unit = {},
    private val onItemClick: (TimelineItem) -> Unit = {}
) : RecyclerView.Adapter<UnifiedItemAdapter.UnifiedViewHolder>() {

    private var items = listOf<TimelineItem>()

    fun setData(tareas: List<Tarea>, eventos: List<Evento>) {
        val timelineItems = mutableListOf<TimelineItem>()

        // Convertir tareas a timeline items
        timelineItems.addAll(tareas.map { it.toTimelineItem() })

        // Convertir eventos a timeline items
        timelineItems.addAll(eventos.map { it.toTimelineItem() })

        // Ordenar por fecha cronológicamente
        items = timelineItems.sortedBy { it.getDateForSorting() }

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnifiedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.unified_item_card, parent, false)
        return UnifiedViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnifiedViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount() = items.size

    inner class UnifiedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTitulo: TextView = itemView.findViewById(R.id.text_titulo)
        private val textDesc: TextView = itemView.findViewById(R.id.textodesc)
        private val textAsignatura: TextView = itemView.findViewById(R.id.texto_asignatura)
        private val textFecha: TextView = itemView.findViewById(R.id.textofecha)
        private val typeBadge: TextView = itemView.findViewById(R.id.item_type_badge)
        private val priorityIndicator: View = itemView.findViewById(R.id.priority_indicator)
        private val opciones: ImageView = itemView.findViewById(R.id.opciones)

        // Timeline elements
        private val timelineLineTop: View = itemView.findViewById(R.id.timeline_line_top)
        private val timelineLineBottom: View = itemView.findViewById(R.id.timeline_line_bottom)
        private val timelineCircle: View = itemView.findViewById(R.id.timeline_circle)

        fun bind(item: TimelineItem) {
            textTitulo.text = item.titulo
            textDesc.text = item.descrip
            textFecha.text = formatDisplayDate(item.fecha)

            // Configure based on item type
            when (item.type) {
                TimelineItemType.TAREA -> {
                    textAsignatura.text = item.asignatura
                    textAsignatura.visibility = View.VISIBLE
                    typeBadge.text = "TAREA"
                    typeBadge.setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))
                }
                TimelineItemType.EVENTO -> {
                    textAsignatura.visibility = View.GONE
                    typeBadge.text = "EVENTO"
                    typeBadge.setBackgroundColor(android.graphics.Color.parseColor("#673AB7"))
                }
            }

            // Set priority indicator color
            val priorityColor = when (item.prioridad) {
                3 -> "#4CAF50" // High priority - Green
                2 -> "#FF9800" // Medium priority - Orange
                1 -> "#F44336" // Low priority - Red
                else -> "#9C27B0" // Default purple
            }
            priorityIndicator.setBackgroundColor(android.graphics.Color.parseColor(priorityColor))

            // Configure timeline visibility based on position
            val position = adapterPosition
            val isFirst = position == 0
            val isLast = position == items.size - 1

            // Hide top line for first item, bottom line for last item
            timelineLineTop.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
            timelineLineBottom.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

            // Circle is always visible
            timelineCircle.visibility = View.VISIBLE

            // Set click listeners
            itemView.setOnClickListener {
                onItemClick(item)
            }

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

        private fun showPopupMenu(view: View, item: TimelineItem) {
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

        private fun showDeleteDialog(item: TimelineItem) {
            val builder = AlertDialog.Builder(context)

            when (item.type) {
                TimelineItemType.TAREA -> {
                    builder.setPositiveButton("Sí") { _, _ ->
                        val tarea = Tarea(item.id, item.titulo, item.descrip, item.asignatura ?: "", item.fecha, item.prioridad)
                        onDeleteTarea(tarea)
                        Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    }
                    builder.setNegativeButton("No") { _, _ -> }
                    builder.setTitle("¿Eliminar Tarea?")
                    builder.setMessage("¿Está seguro que desea eliminar esta tarea?")
                }
                TimelineItemType.EVENTO -> {
                    builder.setPositiveButton("Sí") { _, _ ->
                        val evento = Evento(item.id, item.titulo, item.descrip, item.fecha, item.prioridad)
                        onDeleteEvento(evento)
                        Toast.makeText(context, "Evento eliminado", Toast.LENGTH_SHORT).show()
                    }
                    builder.setNegativeButton("No") { _, _ -> }
                    builder.setTitle("¿Eliminar Evento?")
                    builder.setMessage("¿Está seguro que desea eliminar este evento?")
                }
            }

            builder.create().show()
        }
    }
}