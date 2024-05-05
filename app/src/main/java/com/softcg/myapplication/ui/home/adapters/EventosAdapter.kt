package com.softcg.myapplication.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.evento.model.Evento
import com.softcg.myapplication.ui.tarea.model.Tarea

class EventosAdapter : RecyclerView.Adapter<EventosAdapter.MyViewHolder>(){

    private var eventoslist = emptyList<Evento>()

    class MyViewHolder(itemeventoView: View) :RecyclerView.ViewHolder(itemeventoView) {
        val textTitulo : TextView = itemeventoView.findViewById(R.id.text_titulo)
        val textDesc : TextView = itemeventoView.findViewById(R.id.textodesc)
        val textFecha : TextView = itemeventoView.findViewById(R.id.textofecha)
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
    }
    fun setData(evento: List<Evento>){
        this.eventoslist=evento
        notifyDataSetChanged()
    }

}