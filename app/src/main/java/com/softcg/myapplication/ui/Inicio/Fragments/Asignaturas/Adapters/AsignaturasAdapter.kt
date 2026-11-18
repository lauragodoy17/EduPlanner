package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.confirmar
import com.softcg.myapplication.core.ex.mostrarAviso
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.AsignaturasViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.ResumenDatos
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models.AsignaturaConHora

class AsignaturasAdapter (
    private val context: Context,
    private val inicioViewModel: AsignaturasViewModel,
    private val onVerDatos: (Asignatura) -> Unit = {}
) : RecyclerView.Adapter<AsignaturasAdapter.MyViewHolder>()  {

    private var asignaturas = emptyList<Asignatura>()
    private var resumenes = emptyMap<Int, ResumenDatos>()

    fun setResumenes(resumenes: Map<Int, ResumenDatos>) {
        this.resumenes = resumenes
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val textNombre : TextView = itemView.findViewById(R.id.text_titulo)
        val textTutor : TextView = itemView.findViewById(R.id.textodesc)
        val textHora : TextView = itemView.findViewById(R.id.text_hora)
        val textDuracion : TextView = itemView.findViewById(R.id.text_duracion)
        val textDias : TextView = itemView.findViewById(R.id.text_dias)
        val boton: ImageView = itemView.findViewById(R.id.Opciones)
        val colorBar: View = itemView.findViewById(R.id.card_color_bar)
        val mainCard: CardView = itemView.findViewById(R.id.main_card)
        val datos: View = itemView.findViewById(R.id.datos_prediccion)
        val textDatos: TextView = itemView.findViewById(R.id.text_datos_prediccion)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_asignatura_modern, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return asignaturas.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem=asignaturas[position]
        holder.textNombre.text = currentItem.nombre
        holder.textTutor.text = currentItem.tutor
        holder.textHora.text = AsignaturaConHora.horaLegible(currentItem.hora)
        holder.textDuracion.text = "${currentItem.duracion}h"

        // Generate days string from horario array
        val dias = mutableListOf<String>()
        val diasNombres = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        currentItem.horario.forEachIndexed { index, isActive ->
            if (isActive && index < diasNombres.size) {
                dias.add(diasNombres[index])
            }
        }
        holder.textDias.text = if (dias.isNotEmpty()) dias.joinToString(", ") else "Sin días asignados"

        // Color que identifica la asignatura
        holder.colorBar.backgroundTintList = ColorStateList.valueOf(Color.parseColor(AsignaturaConHora.getColorAsignatura(position)))

        holder.boton.setOnClickListener {v ->
            showPopupMenu(v,position)
        }

        // Datos para predicciones: cuántos hay y, cada mes, el recordatorio de confirmar el interés
        val resumen = currentItem.id?.let { resumenes[it] }
        holder.datos.visibility = if (resumen != null) View.VISIBLE else View.GONE
        if (resumen != null) {
            holder.textDatos.text = when {
                resumen.actualizarInteres -> "¿Sigue igual tu interés? · Datos ${resumen.listos}/${resumen.total}"
                resumen.listos == resumen.total -> "Datos para predicciones completos"
                else -> "Datos para predicciones: ${resumen.listos}/${resumen.total}"
            }
            holder.datos.setOnClickListener { onVerDatos(currentItem) }
        }

        // Add animation for better UX
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())
            .start()
    }

    private fun showPopupMenu(view: View,position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menudos)
        popupMenu.menu.findItem(R.id.VerMas).isVisible = false

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.Borrar -> {
                    deleteItem(position, view)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    fun setData(item: List<Asignatura>){
        this.asignaturas=item
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int, ancla: View){
        val currentItem= asignaturas[position]
        context.confirmar("¿Eliminar asignatura?", "Esta acción no se puede deshacer.", "Eliminar") {
            inicioViewModel.deleteAsignatura(currentItem)
            ancla.mostrarAviso("Asignatura eliminada")
        }
    }

}