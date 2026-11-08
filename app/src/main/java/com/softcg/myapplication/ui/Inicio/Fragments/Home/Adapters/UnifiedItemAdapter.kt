package com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.confirmar
import com.softcg.myapplication.core.ex.estiloPrioridad
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.core.ex.pintarEtiqueta
import com.softcg.myapplication.core.ex.pintarPrioridad
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.core.ex.tintarFondo
import com.softcg.myapplication.ui.Inicio.Models.*
import java.text.SimpleDateFormat
import java.util.*

class UnifiedItemAdapter(
    private val context: Context,
    private val onDeleteTarea: (Tarea) -> Unit = {},
    private val onDeleteEvento: (Evento) -> Unit = {},
    private val onEditEvento: (Evento) -> Unit = {},
    private val onCalificarTarea: (Tarea) -> Unit = {},
    private val onEntregarTarea: (Tarea) -> Unit = {},
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
            itemView.setOnClickListener {
                onItemClick(item)
                if (item.type == TimelineItemType.EVENTO) showEventoDetallesDialog(eventoDe(item))
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
                    typeBadge.pintarEtiqueta("TAREA", R.color.purple_accent, R.color.purple_surface)
                    pintarIcono(R.drawable.ic_gami_tarea, R.color.purple_accent, R.color.purple_surface)
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

        private fun formatCalificacion(valor: Float): String =
            if (valor % 1f == 0f) valor.toInt().toString() else valor.toString()

        private fun showPopupMenu(view: View, item: TimelineItem) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menudos)
            // Solo los eventos tienen pantalla de detalle
            popupMenu.menu.findItem(R.id.VerMas).isVisible = item.type == TimelineItemType.EVENTO
            popupMenu.menu.findItem(R.id.Calificar).isVisible =
                item.type == TimelineItemType.TAREA && item.calificacion == null
            popupMenu.menu.findItem(R.id.Entregar).isVisible =
                item.type == TimelineItemType.TAREA && item.calificacion == null && item.entregadaMillis == null

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.Calificar -> {
                        onCalificarTarea(Tarea(item.id, item.titulo, item.descrip, item.asignatura ?: "", item.fecha, item.prioridad, entregadaMillis = item.entregadaMillis))
                        true
                    }
                    R.id.Entregar -> {
                        onEntregarTarea(Tarea(item.id, item.titulo, item.descrip, item.asignatura ?: "", item.fecha, item.prioridad))
                        true
                    }
                    R.id.VerMas -> {
                        showEventoDetallesDialog(eventoDe(item))
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

            pintarPrioridad(evento.prioridad, prioridadIndicador, prioridadTexto)
            estiloPrioridad(evento.prioridad)?.let { prioridadTexto.setTextColor(context.getColor(it.color)) }

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
                confirmarBorrarEvento(evento)
            }

            // Botón Cerrar
            btnCerrar.setOnClickListener {
                dialog.dismiss()
            }

            dialog.showAsBottomSheet()
        }
    }
}