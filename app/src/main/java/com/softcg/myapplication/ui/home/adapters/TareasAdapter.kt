package com.softcg.myapplication.ui.home.adapters

import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.tarea.model.Tarea


class TareasAdapter : RecyclerView.Adapter<TareasAdapter.MyViewHolder>() {

    private var tareaslist= emptyList<Tarea>()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val textTitulo : TextView = itemView.findViewById(R.id.text_titulo)
        val textDesc : TextView = itemView.findViewById(R.id.textodesc)
        val textFecha : TextView = itemView.findViewById(R.id.textofecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_list,parent,false))
    }

    override fun getItemCount(): Int {
        return tareaslist.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem=tareaslist[position]
        holder.textTitulo.text=currentItem.titulo
        holder.textDesc.text=currentItem.descrip
        holder.textFecha.text=currentItem.asignatura
    }
    fun setData(tarea: List<Tarea>){
        this.tareaslist=tarea
        notifyDataSetChanged()
    }


}
