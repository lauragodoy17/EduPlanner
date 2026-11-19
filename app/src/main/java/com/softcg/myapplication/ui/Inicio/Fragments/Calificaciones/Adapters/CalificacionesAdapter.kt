package com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Adapters

import android.content.Context
import android.content.res.ColorStateList
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
import com.softcg.myapplication.domain.calificaciones.TipoEscala
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.CalificacionesViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Calificaciones.Models.Calificacion
import com.softcg.myapplication.domain.compartir.ContenidoCompartido
import com.softcg.myapplication.ui.Inicio.Dialogs.CompartirQrDialog

class CalificacionesAdapter (
    private val context: Context,
    private val inicioViewModel: CalificacionesViewModel,
    /** Tocar la tarjeta abre la pantalla de la calificación. */
    private val onAbrir: (Calificacion) -> Unit = {}
) : RecyclerView.Adapter<CalificacionesAdapter.MyViewHolder>() {

    private var calificaciones= emptyList<Calificacion>()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val textTitulo : TextView = itemView.findViewById(R.id.text_titulo)
        val textDesc : TextView = itemView.findViewById(R.id.textodesc)
        val textCalificacion: TextView =itemView.findViewById(R.id.textOverlay)
        val boton: ImageButton = itemView.findViewById(R.id.Opciones)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_calificacion_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return calificaciones.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem=calificaciones[position]
        holder.textTitulo.text=currentItem.asignatura
        holder.textDesc.text=currentItem.descripcion
        holder.textDesc.visibility = if (currentItem.descripcion.isBlank()) View.GONE else View.VISIBLE
        pintarNota(holder.textCalificacion, currentItem)
        holder.boton.setOnClickListener {v ->
            showPopupMenu(v,position)
        }
        holder.itemView.setOnClickListener { onAbrir(currentItem) }
    }

    /**
     * La nota en su escala dentro de una pastilla de ancho fijo con texto que se ajusta ("Aprobado" no cabía en el círculo).
     * Aprobado/reprobado se pintan de éxito o error; las demás con el acento (su nota mínima para aprobar varía).
     */
    private fun pintarNota(vista: TextView, nota: Calificacion) {
        val texto = nota.textoValor
        vista.text = texto
        val (color, fondo) = when {
            nota.escala?.tipo == TipoEscala.APROBADO && nota.valor >= 1f -> R.color.estado_exito to R.color.estado_exito_suave
            nota.escala?.tipo == TipoEscala.APROBADO -> R.color.estado_error to R.color.estado_error_suave
            else -> R.color.acento to R.color.marca_superficie
        }
        vista.setTextColor(context.getColor(color))
        vista.backgroundTintList = ColorStateList.valueOf(context.getColor(fondo))
        vista.contentDescription = "Calificación $texto"
    }

    private fun showPopupMenu(view: View,position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menudos)
        popupMenu.menu.findItem(R.id.VerMas).isVisible = false

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.Compartir -> {
                    calificaciones.getOrNull(position)?.let { CompartirQrDialog(context, ContenidoCompartido.de(it)).show() }
                    true
                }
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