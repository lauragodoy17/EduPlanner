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
            val color = if (esEvento) R.color.evento else R.color.purple_accent
            val fondo = if (esEvento) R.color.evento_suave else R.color.purple_surface
            typeBadge.pintarEtiqueta(if (esEvento) "EVENTO" else "TAREA", color, fondo)
            icono.setImageResource(if (esEvento) R.drawable.ic_gami_evento else R.drawable.ic_gami_tarea)
            icono.imageTintList = ColorStateList.valueOf(context.getColor(color))
            icono.tintarFondo(fondo)

            pintarPrioridad(item.prioridad, priorityIndicator, prioridadTexto)

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
            popupMenu.menu.findItem(R.id.VerMas).isVisible = false

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
