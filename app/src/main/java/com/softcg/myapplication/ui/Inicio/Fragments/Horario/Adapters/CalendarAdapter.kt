package com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import java.time.LocalDate
import kotlin.collections.ArrayList

class CalendarAdapter(
    private val context: Context,
    private val days: ArrayList<LocalDate>
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {
    private var currentDate: LocalDate= LocalDate.now()
    class CalendarViewHolder( itemView: View) : RecyclerView.ViewHolder(itemView) {
        val parentView: View = itemView.findViewById(R.id.parentView)
        val dayOfMonth: TextView = itemView.findViewById(R.id.cellDayText)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.calendar_cell, parent, false)
        val layoutParams = view.layoutParams

        layoutParams.height = if (days.size > 15) {
            (parent.height * 0.166666666).toInt() // Month view
        } else {
            parent.height // Week view
        }

        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        if (days[position]==null){
            holder.dayOfMonth.text =""
            holder.parentView.setBackgroundColor(
                Color.LTGRAY
            )
        }else{
            val date = days[position]
            holder.dayOfMonth.text = date.dayOfMonth.toString()
            holder.parentView.setBackgroundColor(
                Color.TRANSPARENT
            )
            holder.parentView.setOnClickListener{v ->
                selectDate(date)

            }
            if(days[position]==currentDate){
                holder.parentView.setBackgroundColor(Color.argb(255, 219, 228, 221))
            }
        }

    }

    override fun getItemCount(): Int = days.size

    private fun selectDate(date: LocalDate){
        currentDate=date
        setData()
        Toast.makeText(context, "Clases del ${date.dayOfMonth} de ${date.month}", Toast.LENGTH_SHORT).show()
    }
    fun setData(){
        notifyDataSetChanged()
    }
}