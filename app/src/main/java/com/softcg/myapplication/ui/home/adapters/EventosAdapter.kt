package com.softcg.myapplication.ui.home.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.evento.model.Evento
import com.softcg.myapplication.ui.home.ViewModelHome
import com.softcg.myapplication.ui.tarea.model.Tarea

class EventosAdapter(private val context: Context, private val viewModelHome: ViewModelHome) : RecyclerView.Adapter<EventosAdapter.MyViewHolder>(){

    private var eventoslist = emptyList<Evento>()

    class MyViewHolder(itemeventoView: View) :RecyclerView.ViewHolder(itemeventoView) {
        val textTitulo : TextView = itemeventoView.findViewById(R.id.text_titulo)
        val textDesc : TextView = itemeventoView.findViewById(R.id.textodesc)
        val textFecha : TextView = itemeventoView.findViewById(R.id.textofecha)
        val boton : ImageButton = itemeventoView.findViewById(R.id.Opciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventosAdapter.MyViewHolder {
        return EventosAdapter.MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_evento_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return eventoslist.size
    }

    override fun onBindViewHolder(holder: EventosAdapter.MyViewHolder, position: Int) {
        val currentItem=eventoslist[position]
        holder.textTitulo.text=currentItem.titulo
        holder.textDesc.text=currentItem.descrip
        holder.textFecha.text=currentItem.fecha
        holder.boton.setOnClickListener { v ->
            showPopupMenu(v,position)
        }
    }

    private fun showPopupMenu(view: View,position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menudos)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.Borrar -> {
                    deleteEvento(position)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }
    fun setData(evento: List<Evento>){
        this.eventoslist=evento
        notifyDataSetChanged()
    }

    fun deleteEvento(position: Int){
        val builder = AlertDialog.Builder(context)
        val currentItem= eventoslist[position]
        builder.setPositiveButton("Si"){ _, _ ->
            viewModelHome.deleteEvento(currentItem)
            Toast.makeText(context,"Evento Borrado :)", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No"){ _, _ ->}
        builder.setTitle("Borrar Evento?")
        builder.setMessage("Esta seguro que desea borrar este evento?")
        builder.create().show()
    }

}