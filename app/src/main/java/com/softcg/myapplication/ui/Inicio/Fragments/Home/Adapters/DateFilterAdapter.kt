package com.softcg.myapplication.ui.Inicio.Fragments.Home.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Models.DateFilterItem

class DateFilterAdapter(
    private val onDateSelected: (DateFilterItem) -> Unit
) : RecyclerView.Adapter<DateFilterAdapter.DateFilterViewHolder>() {

    private var dateList = listOf<DateFilterItem>()
    private var selectedPosition = -1

    fun setData(dates: List<DateFilterItem>) {
        this.dateList = dates
        selectedPosition = dates.indexOfFirst { it.isToday }
        notifyDataSetChanged()
    }

    fun selectDate(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateFilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_filter, parent, false)
        return DateFilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateFilterViewHolder, position: Int) {
        val dateItem = dateList[position]
        holder.bind(dateItem, position == selectedPosition)

        holder.itemView.setOnClickListener {
            selectDate(position)
            onDateSelected(dateItem)
        }
    }

    override fun getItemCount(): Int = dateList.size

    class DateFilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContainer: ViewGroup = itemView.findViewById(R.id.card_container)
        private val tvDayName: TextView = itemView.findViewById(R.id.tv_day_name)
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)

        fun bind(dateItem: DateFilterItem, isSelected: Boolean) {
            tvDayName.text = dateItem.dayName
            tvDayNumber.text = dateItem.dayNumber

            val context = itemView.context

            if (isSelected) {
                cardContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.purple_primary))
                tvDayName.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                tvDayNumber.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            } else if (dateItem.isToday) {
                cardContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.purple_light))
                tvDayName.setTextColor(ContextCompat.getColor(context, R.color.purple_primary))
                tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.purple_primary))
            } else {
                cardContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.input_background))
                tvDayName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }
    }
}