package com.softcg.myapplication.ui.Inicio.Fragments.Horario

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.CalendarUtils.daysInMonthArray
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.CalendarUtils.selectedDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class ScheduleFragment : Fragment() {
    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view= inflater.inflate(R.layout.activity_week_view, container, false)
        CalendarUtils.selectedDate = LocalDate.now()
        initWidgets(view)
        setWeekView()
        return view
    }
    private fun initWidgets(view: View) {
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView)
        monthYearText = view.findViewById(R.id.monthYearTV)
        val botonAdelante =view.findViewById<Button>(R.id.BotonAdelante)
        val botonAtras =view.findViewById<Button>(R.id.BotonAnterior)
        botonAdelante.setOnClickListener{
            nextWeekAction()
        }
        botonAtras.setOnClickListener{
            previousWeekAction()
        }
    }

    private fun setWeekView() {
        monthYearText.text = CalendarUtils.selectedDate?.let { CalendarUtils.monthYearFromDate(it) }
        val days = CalendarUtils.selectedDate?.let { CalendarUtils.daysInWeekArray(it) }
        val calendarAdapter = days?.let { CalendarAdapter(requireContext(), it) }
        val layoutManager = GridLayoutManager(requireContext(), 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }


    fun previousWeekAction() {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate?.minusWeeks(1)!!
        setWeekView()
    }

    fun nextWeekAction() {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate?.plusWeeks(1)!!
        setWeekView()
    }



}