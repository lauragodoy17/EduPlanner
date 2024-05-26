package com.softcg.myapplication.ui.Inicio.Fragments.Horario

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import java.time.LocalDate
import kotlin.collections.ArrayList

class CalendarAdapter(
    private val days: ArrayList<LocalDate>,
    private val onItemListener: OnItemListener
) : RecyclerView.Adapter<CalendarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.calendar_cell, parent, false)
        val layoutParams = view.layoutParams

        layoutParams.height = if (days.size > 15) {
            (parent.height * 0.166666666).toInt() // Month view
        } else {
            parent.height // Week view
        }

        return CalendarViewHolder(view, onItemListener, days)
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
        }
    }

    override fun getItemCount(): Int = days.size

    interface OnItemListener {
        fun onItemClick(position: Int, date: LocalDate?)
    }
}