package com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
    private val onEditEvento: (Evento) -> Unit = {},
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
                    textAsignatura.visibility = View.INVISIBLE
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
                    R.id.VerMas -> {
                        when (item.type) {
                            TimelineItemType.EVENTO -> {
                                val evento = Evento(
                                    item.id,
                                    item.titulo,
                                    item.descrip,
                                    item.fecha,
                                    item.prioridad,
                                    item.horaInicio ?: "",
                                    item.horaFin ?: "",
                                    item.imagenUri
                                )
                                showEventoDetallesDialog(evento)
                            }
                            TimelineItemType.TAREA -> {
                                // Por ahora solo para eventos
                                showDeleteDialog(item)
                            }
                        }
                        true
                    }
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
                        val evento = Evento(item.id, item.titulo, item.descrip, item.fecha, item.prioridad, item.horaInicio ?: "", item.horaFin ?: "", item.imagenUri)
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

        private fun showEventoDetallesDialog(evento: Evento) {
            val dialog = android.app.Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_evento_detalles)

            // Referencias a los elementos del diálogo
            val titulo = dialog.findViewById<TextView>(R.id.eventoTitulo)
            val descripcion = dialog.findViewById<TextView>(R.id.eventoDescripcion)
            val labelDescripcion = dialog.findViewById<TextView>(R.id.labelDescripcion)
            val fecha = dialog.findViewById<TextView>(R.id.eventoFecha)
            val horario = dialog.findViewById<TextView>(R.id.eventoHorario)
            val labelHorario = dialog.findViewById<TextView>(R.id.labelHorario)
            val containerHorario = dialog.findViewById<View>(R.id.containerHorario)
            val prioridadIndicador = dialog.findViewById<View>(R.id.prioridadIndicador)
            val prioridadTexto = dialog.findViewById<TextView>(R.id.prioridadTexto)
            val imagenEvento = dialog.findViewById<ImageView>(R.id.eventoImagen)
            val imageContainer = dialog.findViewById<View>(R.id.imageContainer)

            val btnEditar = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditar)
            val btnEliminar = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEliminar)
            val btnCerrar = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCerrar)

            // Configurar datos
            titulo.text = evento.titulo

            // Manejar descripción
            if (evento.descrip.isNotEmpty()) {
                descripcion.text = evento.descrip
                descripcion.visibility = View.VISIBLE
                labelDescripcion.visibility = View.VISIBLE
            } else {
                descripcion.visibility = View.GONE
                labelDescripcion.visibility = View.GONE
            }

            fecha.text = formatDisplayDate(evento.fecha)

            // Mostrar horario si existe
            if (evento.horaInicio.isNotEmpty() && evento.horaFin.isNotEmpty()) {
                horario.text = "${evento.horaInicio} - ${evento.horaFin}"
                labelHorario.visibility = View.VISIBLE
                containerHorario.visibility = View.VISIBLE
            } else if (evento.horaInicio.isNotEmpty()) {
                horario.text = evento.horaInicio
                labelHorario.visibility = View.VISIBLE
                containerHorario.visibility = View.VISIBLE
            } else {
                labelHorario.visibility = View.GONE
                containerHorario.visibility = View.GONE
            }

            // Configurar prioridad
            when (evento.prioridad) {
                1 -> {
                    prioridadIndicador.setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
                    prioridadTexto.text = "Alta"
                    prioridadTexto.setTextColor(android.graphics.Color.parseColor("#F44336"))
                }
                2 -> {
                    prioridadIndicador.setBackgroundColor(android.graphics.Color.parseColor("#FFEB3B"))
                    prioridadTexto.text = "Media"
                    prioridadTexto.setTextColor(android.graphics.Color.parseColor("#F57C00"))
                }
                3 -> {
                    prioridadIndicador.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                    prioridadTexto.text = "Baja"
                    prioridadTexto.setTextColor(android.graphics.Color.parseColor("#2196F3"))
                }
            }

            // Mostrar imagen si existe
            if (!evento.imagenUri.isNullOrEmpty()) {
                try {
                    val uri = android.net.Uri.parse(evento.imagenUri)
                    imagenEvento.setImageURI(uri)
                    imageContainer.visibility = View.VISIBLE
                } catch (e: Exception) {
                    imageContainer.visibility = View.GONE
                }
            } else {
                imageContainer.visibility = View.GONE
            }

            // Botón Editar
            btnEditar.setOnClickListener {
                dialog.dismiss()
                onEditEvento(evento)
            }

            // Botón Eliminar
            btnEliminar.setOnClickListener {
                dialog.dismiss()
                val builder = AlertDialog.Builder(context)
                builder.setTitle("¿Eliminar Evento?")
                builder.setMessage("¿Está seguro que desea eliminar este evento?")
                builder.setPositiveButton("Sí") { _, _ ->
                    onDeleteEvento(evento)
                    Toast.makeText(context, "Evento eliminado", Toast.LENGTH_SHORT).show()
                }
                builder.setNegativeButton("No") { _, _ -> }
                builder.create().show()
            }

            // Botón Cerrar
            btnCerrar.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
    }
}