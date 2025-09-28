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

        horarioViewModel._asignaturas.observe(viewLifecycleOwner, Observer { Asignatura ->
            adapter.setData(Asignatura)
            if (Asignatura.isNotEmpty()) {
                view.findViewById<TextView>(R.id.textoAsignaturas).visibility = View.GONE
            } else{
                view.findViewById<TextView>(R.id.textoAsignaturas).visibility = View.VISIBLE
            }
        })

        horarioViewModel._currentDate.observe(viewLifecycleOwner, Observer {date ->
            horarioViewModel.obtenerAsignaturas()
        })
    }

    override fun onItemClick(date: LocalDate) {
        horarioViewModel.obtenerCurrentDate(date)
        horarioViewModel.obtenerAsignaturas()
    }


}