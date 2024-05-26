package com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters

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
import com.softcg.myapplication.ui.Inicio.Fragments.Home.HomeViewModel
import com.softcg.myapplication.ui.Inicio.Models.Tarea


class TareasAdapter(private val context: Context, private val inicioViewModel: HomeViewModel) : RecyclerView.Adapter<TareasAdapter.MyViewHolder>() {

    private var tareaslist= emptyList<Tarea>()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val textTitulo : TextView = itemView.findViewById(R.id.text_titulo)
        val textDesc : TextView = itemView.findViewById(R.id.textodesc)
        val textasignatura:TextView=itemView.findViewById(R.id.texto_asignatura)
        val textFecha : TextView = itemView.findViewById(R.id.textofecha)
        val boton: ImageButton = itemView.findViewById(R.id.Opciones)
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
        holder.textasignatura.text=currentItem.asignatura
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
                    deleteTarea(position)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    fun setData(tarea: List<Tarea>){
        this.tareaslist=tarea
        notifyDataSetChanged()
    }

    fun deleteTarea(position: Int){
        val builder = AlertDialog.Builder(context)
        val currentItem= tareaslist[position]
        builder.setPositiveButton("Si"){ _, _ ->
            inicioViewModel.deleteTarea(currentItem)
            Toast.makeText(context,"Tarea Borrada ;D",Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No"){ _, _ ->}
        builder.setTitle("Borrar Tarea?")
        builder.setMessage("Esta seguro que desea borrar esta tarea?")
        builder.create().show()
    }
}
