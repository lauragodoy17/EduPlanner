package com.softcg.myapplication.ui.Inicio.Fragments.Horario

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters.CalendarAdapter
import com.softcg.myapplication.core.utils.CalendarUtils
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters.HorarioAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Horario.Adapters.OnItemClickListener
import com.softcg.myapplication.ui.Inicio.Fragments.Agenda.AgendaCalendarWidget
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ScheduleFragment : Fragment(), OnItemClickListener {

    private val horarioViewModel: HorarioViewModel by viewModels()
    private lateinit var calendarWidget: AgendaCalendarWidget
    private lateinit var adapter: HorarioAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_week_view, container, false)
        initCalendarWidget(view)
        initTimeline(view)
        initRecyclerAsignatura(view)
        return view
    }

    private fun initCalendarWidget(view: View) {
        try {
            calendarWidget = view.findViewById(R.id.schedule_calendar_widget)

            calendarWidget.onDateSelectedListener = { selectedDate ->
                // Convert string date to LocalDate for horario functionality
                convertAndFilterByDate(selectedDate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun convertAndFilterByDate(selectedDate: String) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(selectedDate)
            val calendar = Calendar.getInstance()
            calendar.time = date!!

            val localDate = LocalDate.of(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            horarioViewModel.obtenerCurrentDate(localDate)
            horarioViewModel.obtenerAsignaturas()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun initRecyclerAsignatura(view: View){
        horarioViewModel.obtenerAsignaturas()
        adapter = HorarioAdapter()
        val recyclerView = view.findViewById<RecyclerView>(R.id.horarioRecyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        horarioViewModel._asignaturas.observe(viewLifecycleOwner, Observer { asignaturas ->
            adapter.setDataFromAsignaturas(asignaturas)
            if (asignaturas.isNotEmpty()) {
                view.findViewById<TextView>(R.id.textoAsignaturas).visibility = View.GONE
            } else{
                view.findViewById<TextView>(R.id.textoAsignaturas).visibility = View.VISIBLE
            }
        })

        horarioViewModel._currentDate.observe(viewLifecycleOwner, Observer {date ->
            horarioViewModel.obtenerAsignaturas()
        })
    }

    private fun initTimeline(view: View) {
        val timeColumn = view.findViewById<LinearLayout>(R.id.time_column)

        // Clear any existing time slots
        timeColumn.removeAllViews()

        // Generate time slots from 7:00 AM to 9:00 PM
        val timeSlots = generateTimeSlots()

        timeSlots.forEach { timeSlot ->
            val timeView = TextView(requireContext())
            timeView.text = timeSlot
            timeView.textSize = 12f
            timeView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
            timeView.gravity = android.view.Gravity.CENTER
            timeView.setPadding(8, 16, 8, 16)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 8)
            timeView.layoutParams = params

            timeColumn.addView(timeView)
        }
    }

    private fun generateTimeSlots(): List<String> {
        val timeSlots = mutableListOf<String>()
        for (hour in 7..21) {
            timeSlots.add(String.format("%02d:00", hour))
        }
        return timeSlots
    }

    override fun onItemClick(date: LocalDate) {
        horarioViewModel.obtenerCurrentDate(date)
        horarioViewModel.obtenerAsignaturas()
    }


}