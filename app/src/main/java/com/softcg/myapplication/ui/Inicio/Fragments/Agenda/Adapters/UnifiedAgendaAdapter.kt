package com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Adapters

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
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.AgendaViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.AgendaItem
import com.softcg.myapplication.ui.Inicio.Models.Tarea
import java.text.SimpleDateFormat
import java.util.*

class UnifiedAgendaAdapter(
    private val context: Context,
    private val agendaViewModel: AgendaViewModel,
    private val onVerMasTarea: (Tarea) -> Unit = {},
    /** Tocar la fila abre la pantalla de la tarea o del evento. */
    private val onAbrir: (AgendaItem) -> Unit = {}
) : RecyclerView.Adapter<UnifiedAgendaAdapter.UnifiedAgendaViewHolder>() {

    private var agendaList = emptyList<AgendaItem>()

    fun setData(items: List<AgendaItem>) {
        // Sort agenda items by date
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        agendaList = items.sortedBy { item -> runCatching { formato.parse(item.fecha) }.getOrNull() ?: Date() }
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
        private val prioridadTexto: TextView = itemView.findViewById(R.id.prioridad_texto)
        private val icono: ImageView = itemView.findViewById(R.id.item_icono)
        private val opciones: ImageView = itemView.findViewById(R.id.opciones)

        fun bind(item: AgendaItem) {
            textTitulo.text = item.titulo
            textDesc.text = item.descrip
            textDesc.visibility = if (item.descrip.isBlank()) View.GONE else View.VISIBLE
            textFecha.text = formatDisplayDate(item.fecha)

            // asignatura null = evento, otherwise = tarea
            val esEvento = item.asignatura == null
            textAsignatura.text = item.asignatura
            textAsignatura.visibility = if (esEvento) View.GONE else View.VISIBLE
            val color = if (esEvento) R.color.evento else R.color.acento
            val fondo = if (esEvento) R.color.evento_suave else R.color.marca_superficie
            typeBadge.pintarEtiqueta(if (esEvento) "EVENTO" else "TAREA", color, fondo)
            icono.setImageResource(if (esEvento) R.drawable.ic_gami_evento else R.drawable.ic_gami_tarea)
            icono.imageTintList = ColorStateList.valueOf(context.getColor(color))
            icono.tintarFondo(fondo)

            pintarPrioridad(item.prioridad, priorityIndicator, prioridadTexto)

            itemView.setOnClickListener { onAbrir(item) }

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

        /** "2026-11-20" → "20/11/2026"; un texto que no es fecha se muestra tal cual. */
        private fun formatDisplayDate(dateString: String): String = runCatching {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
            date?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: dateString
        }.getOrDefault(dateString)

        private fun showPopupMenu(view: View, item: AgendaItem) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menudos)
            // Las tareas abren su detalle con las recomendaciones de la IA; los eventos de la agenda no tienen
            popupMenu.menu.findItem(R.id.VerMas).isVisible = item.asignatura != null
            // La fila guarda solo lo básico: la tarea se comparte desde "Ver más", con todos sus datos
            popupMenu.menu.findItem(R.id.Compartir).isVisible = false

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.VerMas -> {
                        onVerMasTarea(Tarea(item.id, item.titulo, item.descrip, item.asignatura ?: "", item.fecha, item.prioridad))
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

        private fun showDeleteDialog(item: AgendaItem) {
            val esEvento = item.asignatura == null
            context.confirmar(
                if (esEvento) "¿Eliminar evento?" else "¿Eliminar tarea?",
                "Esta acción no se puede deshacer.",
                "Eliminar"
            ) {
                if (esEvento) agendaViewModel.deleteEvento(item) else agendaViewModel.deleteTarea(item)
                itemView.mostrarAviso(if (esEvento) "Evento eliminado" else "Tarea eliminada")
            }
        }
    }
}
