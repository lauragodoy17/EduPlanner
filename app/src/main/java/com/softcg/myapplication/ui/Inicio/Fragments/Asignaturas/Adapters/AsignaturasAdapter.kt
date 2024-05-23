package com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.Adapters

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
import com.softcg.myapplication.ui.Inicio.Fragments.Asignaturas.AsignaturasViewModel
import com.softcg.myapplication.ui.Inicio.InicioViewModel
import com.softcg.myapplication.ui.Inicio.Models.Asignatura

class AsignaturasAdapter (private val context: Context, private val inicioViewModel: AsignaturasViewModel) : RecyclerView.Adapter<AsignaturasAdapter.MyViewHolder>()  {

    private var asignaturas = emptyList<Asignatura>()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val textNombre : TextView = itemView.findViewById(R.id.text_titulo)
        val textTutor : TextView = itemView.findViewById(R.id.textodesc)
        val boton: ImageButton = itemView.findViewById(R.id.Opciones)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_asignatura_list, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return asignaturas.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem=asignaturas[position]
        holder.textNombre.text = currentItem.nombre
        holder.textTutor.text=currentItem.tutor
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

    fun setData(item: List<Asignatura>){
        this.asignaturas=item
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int){
        val builder = AlertDialog.Builder(context)
        val currentItem= asignaturas[position]
        builder.setPositiveButton("Si"){ _, _ ->
            inicioViewModel.deleteAsignatura(currentItem)
            Toast.makeText(context,"Asignatura Borrada ;D", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No"){ _, _ ->}
        builder.setTitle("Borrar Asignatura?")
        builder.setMessage("Esta seguro que desea borrar esta Asignatura?")
        builder.create().show()
        }

}