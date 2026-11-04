package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.EstadisticasProgreso
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters.MovimientosAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.PerfilViewModel
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views.ActividadHeatmapView
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views.BarrasSemanaView
import java.time.format.DateTimeFormatter
import java.util.Locale

class EstadisticasTabFragment : Fragment() {

    private val perfilViewModel: PerfilViewModel by viewModels({ requireParentFragment() })
    private val formatoDia = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))
    private var historialCompleto = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tab_estadisticas, container, false)

        val detalleDia = view.findViewById<TextView>(R.id.statsActividadDetalle)
        view.findViewById<ActividadHeatmapView>(R.id.statsActividad).onDiaTocado = { dia, xp ->
            detalleDia.text = "${dia.format(formatoDia)} · $xp XP"
        }

        perfilViewModel.panel.observe(viewLifecycleOwner) { panel -> mostrar(view, panel.estadisticas) }
        initHistorial(view)
        return view
    }

    private fun mostrar(view: View, stats: EstadisticasProgreso) {
        view.findViewById<TextView>(R.id.statsTareas).text = stats.tareasTerminadas.toString()
        view.findViewById<TextView>(R.id.statsATiempo).text = stats.porcentajeATiempo?.let { "$it %" } ?: "—"
        view.findViewById<TextView>(R.id.statsAsistencia).text = stats.porcentajeAsistencia?.let { "$it %" } ?: "—"
        view.findViewById<TextView>(R.id.statsEnfoque).text = formatoHoras(stats.minutosEnfoqueTotal)
        view.findViewById<TextView>(R.id.statsRacha).text = "${stats.mejorRacha} d"
        view.findViewById<TextView>(R.id.statsXp).text = stats.xpTotal.toString()

        view.findViewById<BarrasSemanaView>(R.id.statsBarras).mostrar(stats.ultimos7Dias)
        val diferencia = stats.xpSemana - stats.xpSemanaAnterior
        view.findViewById<TextView>(R.id.statsSemanaResumen).text = buildString {
            append("${stats.xpSemana} XP esta semana")
            if (stats.xpSemanaAnterior > 0) {
                append(" · ")
                append(if (diferencia >= 0) "+$diferencia" else "$diferencia")
                append(" frente a la anterior")
            }
            if (stats.minutosEnfoqueSemana > 0) append(" · ${formatoHoras(stats.minutosEnfoqueSemana)} de enfoque")
        }

        view.findViewById<ActividadHeatmapView>(R.id.statsActividad).mostrar(stats.actividad, perfilViewModel.hoy)
    }

    private fun formatoHoras(minutos: Int): String = when {
        minutos < 60 -> "$minutos min"
        minutos % 60 == 0 -> "${minutos / 60} h"
        else -> "${minutos / 60} h ${minutos % 60}"
    }

    private fun initHistorial(view: View) {
        val adapter = MovimientosAdapter()
        val boton = view.findViewById<Button>(R.id.statsHistorialTodo)
        view.findViewById<RecyclerView>(R.id.statsHistorial).apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        fun refrescar() {
            val movimientos = perfilViewModel.movimientos.value.orEmpty()
            adapter.setData(if (historialCompleto) movimientos else movimientos.take(MOVIMIENTOS_RESUMEN))
            view.findViewById<View>(R.id.statsHistorialVacio).visibility =
                if (movimientos.isEmpty()) View.VISIBLE else View.GONE
            boton.visibility = if (movimientos.size > MOVIMIENTOS_RESUMEN) View.VISIBLE else View.GONE
            boton.text = if (historialCompleto) "Ver menos" else "Ver todo (${movimientos.size})"
        }
        boton.setOnClickListener {
            historialCompleto = !historialCompleto
            refrescar()
        }
        perfilViewModel.movimientos.observe(viewLifecycleOwner) { refrescar() }
    }

    companion object {
        private const val MOVIMIENTOS_RESUMEN = 5
    }
}
