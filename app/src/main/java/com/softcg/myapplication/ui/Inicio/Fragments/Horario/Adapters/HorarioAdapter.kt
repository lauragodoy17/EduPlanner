package com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Models.Asignatura

class HorarioAdapter () : RecyclerView.Adapter<HorarioAdapter.MyViewHolder>(){

    private var asignaturaslist = emptyList<Asignatura>()

    class MyViewHolder(itemeventoView: View) :RecyclerView.ViewHolder(itemeventoView) {
        val textNombre : TextView = itemeventoView.findViewById(R.id.text_titulo)
        val textTutor : TextView = itemeventoView.findViewById(R.id.textodesc)
        val textDuracion : TextView = itemeventoView.findViewById(R.id.textofecha)
        val textUbicacion: TextView =itemeventoView.findViewById(R.id.textoubi)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_clase_horario, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return asignaturaslist.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem=asignaturaslist[position]
        holder.textNombre.text=currentItem.nombre
        holder.textTutor.text=currentItem.tutor
        holder.textDuracion.text=currentItem.duracion.toString()
        holder.textUbicacion.text="Universidad Libre"
    }

    fun setData(asignatura: List<Asignatura>){
        this.asignaturaslist=asignatura
        notifyDataSetChanged()
    }




}