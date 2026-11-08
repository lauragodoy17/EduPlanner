package com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.confirmar
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.CalificacionesViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.ui.Inicio.Fragments.Home.HomeViewModel
import com.softcg.myapplication.ui.Inicio.Models.Tarea

class CalificacionesAdapter (private val context: Context, private val inicioViewModel: CalificacionesViewModel) : RecyclerView.Adapter<CalificacionesAdapter.MyViewHolder>() {

    private var calificaciones= emptyList<Calificacion>()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val textTitulo : TextView = itemView.findViewById(R.id.text_titulo)
        val textDesc : TextView = itemView.findViewById(R.id.textodesc)
        val textCalificacion: TextView =itemView.findViewById(R.id.textOverlay)
        val boton: ImageButton = itemView.findViewById(R.id.Opciones)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalificacionesAdapter.MyViewHolder {
        return CalificacionesAdapter.MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_calificacion_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return calificaciones.size
    }

    override fun onBindViewHolder(holder: CalificacionesAdapter.MyViewHolder, position: Int) {
        val currentItem=calificaciones[position]
        holder.textTitulo.text=currentItem.asignatura
        holder.textDesc.text=currentItem.descripcion
        holder.textDesc.visibility = if (currentItem.descripcion.isBlank()) View.GONE else View.VISIBLE
        holder.textCalificacion.text=currentItem.valor.toString()
        holder.boton.setOnClickListener {v ->
            showPopupMenu(v,position)
        }
    }

    private fun showPopupMenu(view: View,position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menudos)
        popupMenu.menu.findItem(R.id.VerMas).isVisible = false

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.Borrar -> {
                    deleteCalificacion(position, view)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    fun setData(calificacion: List<Calificacion>){
        this.calificaciones=calificacion
        notifyDataSetChanged()
    }

    fun deleteCalificacion(position: Int, ancla: View){
        val currentItem= calificaciones[position]
        context.confirmar("¿Eliminar calificación?", "Esta acción no se puede deshacer.", "Eliminar") {
            inicioViewModel.deleteCalificacion(currentItem)
            ancla.mostrarAviso("Calificación eliminada")
        }
    }

}