package com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Models.*
import java.text.SimpleDateFormat
import java.util.*

class TimelineAdapter(
    private val context: Context,
    private val onItemClick: (TimelineItem) -> Unit = {}
) : RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.timeline_item, parent, false)
        return TimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position == items.size - 1)
    }

    override fun getItemCount() = items.size

    inner class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskTitle: TextView = itemView.findViewById(R.id.task_title)
        private val taskDescription: TextView = itemView.findViewById(R.id.task_description)
        private val taskSubject: TextView = itemView.findViewById(R.id.task_subject)
        private val taskDate: TextView = itemView.findViewById(R.id.task_date)
        private val timelineCircle: CardView = itemView.findViewById(R.id.timeline_circle)
        private val circleInner: View = itemView.findViewById(R.id.circle_inner)
        private val timelineLine: View = itemView.findViewById(R.id.timeline_line)
        private val priorityIndicator: View = itemView.findViewById(R.id.priority_indicator)
        private val typeBadge: TextView = itemView.findViewById(R.id.item_type_badge)

        fun bind(item: TimelineItem, isLast: Boolean) {
            taskTitle.text = item.titulo
            taskDescription.text = item.descrip
            taskDate.text = formatDisplayDate(item.fecha)

            // Hide line for last item
            timelineLine.visibility = if (isLast) View.GONE else View.VISIBLE

            // Configure based on item type
            when (item.type) {
                TimelineItemType.TAREA -> {
                    taskSubject.text = item.asignatura
                    taskSubject.visibility = View.VISIBLE
                    typeBadge.text = "TAREA"
                    typeBadge.setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))
                }
                TimelineItemType.EVENTO -> {
                    taskSubject.visibility = View.GONE
                    typeBadge.text = "EVENTO"
                    typeBadge.setBackgroundColor(android.graphics.Color.parseColor("#673AB7"))
                }
            }

            // Always use purple for circles (morado)
            circleInner.setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))

            // Set priority indicator color
            val priorityColor = when (item.prioridad) {
                3 -> "#F44336" // High priority
                2 -> "#FF9800" // Medium priority
                1 -> "#4CAF50" // Low priority
                else -> "#9C27B0" // Default purple
            }
            priorityIndicator.setBackgroundColor(android.graphics.Color.parseColor(priorityColor))

            // Set click listener
            itemView.setOnClickListener {
                onItemClick(item)
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
    }
}