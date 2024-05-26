package com.softcg.myapplication.ui.Inicio.Fragments.Horario

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.CalendarUtils.daysInWeekArray
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.CalendarUtils.monthYearFromDate
import java.time.LocalDate

class WeekViewActivity : AppCompatActivity(), CalendarAdapter.OnItemListener {

    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_week_view)
        initWidgets()
        setWeekView()
    }

    private fun initWidgets() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        monthYearText = findViewById(R.id.monthYearTV)
    }

    private fun setWeekView() {
        monthYearText.text = CalendarUtils.selectedDate?.let { monthYearFromDate(it) }
        val days = CalendarUtils.selectedDate?.let { daysInWeekArray(it) }
        val calendarAdapter = days?.let { CalendarAdapter(it, this) }
        val layoutManager = GridLayoutManager(applicationContext, 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }

    fun previousWeekAction(view: View) {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate?.minusWeeks(1)!!
        setWeekView()
    }

    fun nextWeekAction() {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate?.plusWeeks(1)!!
        setWeekView()
    }

    override fun onItemClick(position: Int, date: LocalDate?) {
        if (date != null) {
            CalendarUtils.selectedDate = date
            setWeekView()
        }
    }
}