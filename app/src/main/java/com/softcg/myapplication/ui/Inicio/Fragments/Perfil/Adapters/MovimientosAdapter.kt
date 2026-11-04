package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.MovimientoEduCoins
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MovimientosAdapter : RecyclerView.Adapter<MovimientosAdapter.MovimientoViewHolder>() {

    private var movimientos = emptyList<MovimientoEduCoins>()
    private val formatoFecha = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es"))

    fun setData(movimientos: List<MovimientoEduCoins>) {
        this.movimientos = movimientos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MovimientoViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_movimiento, parent, false)
    )

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) = holder.bind(movimientos[position])

    override fun getItemCount() = movimientos.size

    inner class MovimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descripcion: TextView = itemView.findViewById(R.id.movimientoDescripcion)
        private val fecha: TextView = itemView.findViewById(R.id.movimientoFecha)
        private val cantidad: TextView = itemView.findViewById(R.id.movimientoCantidad)

        fun bind(movimiento: MovimientoEduCoins) {
            descripcion.text = movimiento.descripcion
            fecha.text = formatoFecha.format(Date(movimiento.fechaMillis))
            if (movimiento.cantidad >= 0) {
                cantidad.text = "+${movimiento.cantidad}"
                cantidad.setTextColor(itemView.context.getColor(R.color.purple_accent))
            } else {
                cantidad.text = movimiento.cantidad.toString()
                cantidad.setTextColor(android.graphics.Color.parseColor("#F44336"))
            }
        }
    }
}
