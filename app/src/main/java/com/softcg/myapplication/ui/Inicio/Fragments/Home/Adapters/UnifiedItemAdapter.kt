package com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.confirmar
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.core.ex.pintarPrioridad
import com.softcg.myapplication.core.ex.tintarFondo
import com.softcg.myapplication.domain.compartir.ContenidoCompartido
import com.softcg.myapplication.ui.Inicio.Dialogs.CompartirQrDialog
import com.softcg.myapplication.ui.Inicio.Models.*
import java.text.SimpleDateFormat
import java.util.*

class UnifiedItemAdapter(
    private val context: Context,
    private val onDeleteTarea: (Tarea) -> Unit = {},
    private val onDeleteEvento: (Evento) -> Unit = {},
    private val onCalificarTarea: (Tarea) -> Unit = {},
    private val onEntregarTarea: (Tarea) -> Unit = {},
    private val onVerMasTarea: (Tarea) -> Unit = {},
    /** Tocar la tarjeta abre la pantalla de la tarea o del evento. */
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
        private val prioridadTexto: TextView = itemView.findViewById(R.id.prioridad_texto)
        private val icono: ImageView = itemView.findViewById(R.id.item_icono)
        private val opciones: ImageView = itemView.findViewById(R.id.opciones)

        // Timeline elements
        private val timelineLineTop: View = itemView.findViewById(R.id.timeline_line_top)
        private val timelineLineBottom: View = itemView.findViewById(R.id.timeline_line_bottom)
        private val timelineCircle: View = itemView.findViewById(R.id.timeline_circle)

        fun bind(item: TimelineItem) {
            textTitulo.text = item.titulo
            textDesc.text = item.descrip
            textDesc.visibility = if (item.descrip.isBlank()) View.GONE else View.VISIBLE
            textFecha.text = formatDisplayDate(item.fecha)

            pintarTipo(item)
            pintarPrioridad(item.prioridad, priorityIndicator, prioridadTexto)

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
            itemView.setOnClickListener { onItemClick(item) }

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

        private fun pintarTipo(item: TimelineItem) {
            val hecha = item.calificacion != null || item.entregadaMillis != null
            when {
                item.type == TimelineItemType.EVENTO -> {
                    textAsignatura.visibility = View.GONE
                    typeBadge.pintarEtiqueta("EVENTO", R.color.evento, R.color.evento_suave)
                    pintarIcono(R.drawable.ic_gami_evento, R.color.evento, R.color.evento_suave)
                }
                hecha -> {
                    textAsignatura.visibility = View.VISIBLE
                    // Una tarea calificada está completada
                    textAsignatura.text = if (item.calificacion != null) {
                        "${item.asignatura} · Calificación: ${formatCalificacion(item.calificacion)}"
                    } else {
                        item.asignatura
                    }
                    typeBadge.pintarEtiqueta(
                        if (item.calificacion != null) "COMPLETADA" else "ENTREGADA",
                        R.color.estado_exito,
                        R.color.estado_exito_suave
                    )
                    pintarIcono(R.drawable.ic_gami_check, R.color.estado_exito, R.color.estado_exito_suave)
                }
                else -> {
                    textAsignatura.visibility = View.VISIBLE
                    textAsignatura.text = item.asignatura
                    typeBadge.pintarEtiqueta("TAREA", R.color.acento, R.color.marca_superficie)
                    pintarIcono(R.drawable.ic_gami_tarea, R.color.acento, R.color.marca_superficie)
                }
            }
        }

        private fun pintarIcono(imagen: Int, color: Int, fondo: Int) {
            icono.setImageResource(imagen)
            icono.imageTintList = ColorStateList.valueOf(context.getColor(color))
            icono.tintarFondo(fondo)
        }

        private fun eventoDe(item: TimelineItem) = Evento(
            item.id,
            item.titulo,
            item.descrip,
            item.fecha,
            item.prioridad,
            item.horaInicio ?: "",
            item.horaFin ?: "",
            item.imagenUri
        )

        private fun tareaDe(item: TimelineItem) = Tarea(
            item.id,
            item.titulo,
            item.descrip,
            item.asignatura ?: "",
            item.fecha,
            item.prioridad,
            calificacion = item.calificacion,
            entregadaMillis = item.entregadaMillis,
            tema = item.tema,
            asignaturaId = item.asignaturaId
        )

        /** "2026-11-20" → "20/11/2026"; un texto que no es fecha se muestra tal cual. */
        private fun formatDisplayDate(dateString: String): String = runCatching {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
            date?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: dateString
        }.getOrDefault(dateString)

        private fun formatCalificacion(valor: Float): String =
            if (valor % 1f == 0f) valor.toInt().toString() else valor.toString()

        private fun showPopupMenu(view: View, item: TimelineItem) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menudos)
            popupMenu.menu.findItem(R.id.Calificar).isVisible =
                item.type == TimelineItemType.TAREA && item.calificacion == null
            popupMenu.menu.findItem(R.id.Entregar).isVisible =
                item.type == TimelineItemType.TAREA && item.calificacion == null && item.entregadaMillis == null

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.Calificar -> {
                        onCalificarTarea(tareaDe(item))
                        true
                    }
                    R.id.Entregar -> {
                        onEntregarTarea(tareaDe(item))
                        true
                    }
                    R.id.VerMas -> {
                        if (item.type == TimelineItemType.EVENTO) onItemClick(item)
                        else onVerMasTarea(tareaDe(item))
                        true
                    }
                    R.id.Compartir -> {
                        val contenido = if (item.type == TimelineItemType.EVENTO) ContenidoCompartido.de(eventoDe(item))
                        else ContenidoCompartido.de(tareaDe(item))
                        CompartirQrDialog(context, contenido).show()
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
            when (item.type) {
                TimelineItemType.TAREA -> context.confirmar("¿Eliminar tarea?", "Esta acción no se puede deshacer.", "Eliminar") {
                    onDeleteTarea(Tarea(item.id, item.titulo, item.descrip, item.asignatura ?: "", item.fecha, item.prioridad))
                    itemView.mostrarAviso("Tarea eliminada")
                }
                TimelineItemType.EVENTO -> confirmarBorrarEvento(eventoDe(item))
            }
        }

        private fun confirmarBorrarEvento(evento: Evento) {
            context.confirmar("¿Eliminar evento?", "Esta acción no se puede deshacer.", "Eliminar") {
                onDeleteEvento(evento)
                itemView.mostrarAviso("Evento eliminado")
            }
        }
    }
}