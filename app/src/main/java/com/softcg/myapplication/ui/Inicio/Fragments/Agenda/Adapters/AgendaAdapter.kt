package com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.AgendaViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.Models.AgendaItem

class AgendaAdapter(private val context: Context, private val inicioViewModel: AgendaViewModel) : RecyclerView.Adapter<AgendaAdapter.MyViewHolder>() {

    private var agendalist= emptyList<AgendaItem>()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val textTitulo : TextView = itemView.findViewById(R.id.text_titulo)
        val textDesc : TextView = itemView.findViewById(R.id.textodesc)
        val textasignatura: TextView =itemView.findViewById(R.id.texto_asignatura)
        val textFecha : TextView = itemView.findViewById(R.id.textofecha)
        val imagen : ImageView = itemView.findViewById(R.id.circleImage)
        val boton: ImageButton = itemView.findViewById(R.id.Opciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return agendalist.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem=agendalist[position]
        holder.textTitulo.text=currentItem.titulo
        holder.textDesc.text=currentItem.descrip
        if (currentItem.asignatura==null){
            holder.textasignatura.visibility=View.GONE
            holder.imagen.foreground=ContextCompat.getDrawable(context,R.drawable.baseline_calendar_month_24)

        } else{
            holder.textasignatura.text=currentItem.asignatura
            holder.imagen.foreground= ContextCompat.getDrawable(context,R.drawable.baseline_agenda_add_24)
        }
        holder.textFecha.text=currentItem.fecha
        holder.boton.setOnClickListener {v ->
            showPopupMenu(v,position)
        }
    }

    private fun showPopupMenu(view: View,position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menudos)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.Borrar -> {
                    deleteItem(position)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    fun setData(item: List<AgendaItem>){
        this.agendalist=item
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int){
        val builder = AlertDialog.Builder(context)
        val currentItem= agendalist[position]
        if(currentItem.asignatura==null){
            builder.setPositiveButton("Si"){ _, _ ->
                inicioViewModel.deleteEvento(currentItem)
                Toast.makeText(context,"Evento Borrado", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("No"){ _, _ ->}
            builder.setTitle("Borrar Evento?")
            builder.setMessage("Esta seguro que desea borrar este evento?")
            builder.create().show()
        }else{
            builder.setPositiveButton("Si"){ _, _ ->
                inicioViewModel.deleteTarea(currentItem)
                Toast.makeText(context,"Tarea Borrada ;D", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("No"){ _, _ ->}
            builder.setTitle("Borrar Tarea?")
            builder.setMessage("Esta seguro que desea borrar esta tarea?")
            builder.create().show()
        }
    }

}