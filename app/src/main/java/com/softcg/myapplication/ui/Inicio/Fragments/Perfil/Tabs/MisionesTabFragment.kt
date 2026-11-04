package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.core.ex.mostrarRecompensa
import com.softcg.myapplication.domain.gamificacion.MisionesConfig
import com.softcg.myapplication.domain.gamificacion.PanelMisiones
import com.softcg.myapplication.domain.gamificacion.RachaReglas
import com.softcg.myapplication.domain.gamificacion.ResultadoMision
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters.MisionesAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.PerfilViewModel
import java.time.temporal.ChronoUnit

class MisionesTabFragment : Fragment() {

    private val perfilViewModel: PerfilViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tab_misiones, container, false)

        val diarias = MisionesAdapter { perfilViewModel.reclamarMision(it.mision.id) }
        val semanales = MisionesAdapter { perfilViewModel.reclamarMision(it.mision.id) }
        view.findViewById<RecyclerView>(R.id.misionesDiarias).apply {
            adapter = diarias
            layoutManager = LinearLayoutManager(requireContext())
        }
        view.findViewById<RecyclerView>(R.id.misionesSemanales).apply {
            adapter = semanales
            layoutManager = LinearLayoutManager(requireContext())
        }

        view.findViewById<View>(R.id.misionesEnfoque).setOnClickListener {
            findNavController().navigate(R.id.id_focus_fragment)
        }

        perfilViewModel.panel.observe(viewLifecycleOwner) { panel ->
            view.findViewById<TextView>(R.id.misionesMensaje).text = panel.mensaje
            diarias.setData(panel.misiones.diarias)
            semanales.setData(panel.misiones.semanales)
            mostrarResumen(view, panel.misiones)
        }

        perfilViewModel.resultadoMision.observe(viewLifecycleOwner) { evento ->
            when (val resultado = evento.getContentIfNotHandled() ?: return@observe) {
                is ResultadoMision.Reclamada -> requireActivity().mostrarRecompensa(
                    resultado.monedas + resultado.bonus,
                    if (resultado.bonus > 0) "Día completo" else null
                )
                ResultadoMision.YaReclamada -> requireActivity().mostrarMensaje("Ya reclamaste esta misión")
                ResultadoMision.Incompleta -> requireActivity().mostrarMensaje("Esta misión aún no está completa")
                ResultadoMision.NoDisponible -> requireActivity().mostrarMensaje("Esta misión ya terminó")
                ResultadoMision.SinSesion, ResultadoMision.Error ->
                    requireActivity().mostrarMensaje("No se pudo reclamar. Inténtalo de nuevo")
            }
            // Vuelve a habilitar el botón si no se reclamó
            perfilViewModel.refrescar()
        }
        return view
    }

    private fun mostrarResumen(view: View, misiones: PanelMisiones) {
        val total = misiones.diarias.size
        view.findViewById<TextView>(R.id.misionesDiariasResumen).text =
            if (misiones.bonusReclamado) "Día completo · +${MisionesConfig.BONUS_DIARIO} ganados"
            else "${misiones.diariasReclamadas}/$total · Bono +${MisionesConfig.BONUS_DIARIO}"

        val hoy = perfilViewModel.hoy
        val dias = ChronoUnit.DAYS.between(hoy, RachaReglas.lunes(hoy).plusDays(7)).toInt()
        view.findViewById<TextView>(R.id.misionesSemanalesResumen).text =
            if (dias == 1) "Termina hoy" else "Quedan $dias días"
    }
}
