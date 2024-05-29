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
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

@AndroidEntryPoint
class ScheduleFragment : Fragment(), OnItemClickListener {

    private val horarioViewModel: HorarioViewModel by viewModels()

    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view= inflater.inflate(R.layout.activity_week_view, container, false)
        CalendarUtils.selectedDate = LocalDate.now()
        horarioViewModel.obtenerCurrentDate(LocalDate.now())
        initWidgets(view)
        setWeekView()
        initRecyclerAsignatura(view)
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
        val calendarAdapter = days?.let { horarioViewModel._currentDate.value?.let { it1 ->
            CalendarAdapter(requireContext(), this,it,
                it1
            )
        } }
        val layoutManager = GridLayoutManager(requireContext(), 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }

    fun initRecyclerAsignatura(view: View){
        horarioViewModel.obtenerAsignaturas()
        val adapter = HorarioAdapter()
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


    fun previousWeekAction() {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate?.minusWeeks(1)!!
        setWeekView()
    }

    fun nextWeekAction() {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate?.plusWeeks(1)!!
        setWeekView()
    }

    override fun onItemClick(date: LocalDate) {
        horarioViewModel.obtenerCurrentDate(date)
        horarioViewModel.obtenerAsignaturas()

    }


}