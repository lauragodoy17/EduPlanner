package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.softcg.myapplication.R
import com.softcg.myapplication.ui.Inicio.Dialogs.LogroDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters.LogrosAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.PerfilViewModel

class LogrosTabFragment : Fragment() {

    private val perfilViewModel: PerfilViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tab_logros, container, false)

        val adapter = LogrosAdapter { LogroDialog(requireContext(), it).show() }
        view.findViewById<RecyclerView>(R.id.logrosLista).apply {
            this.adapter = adapter
            layoutManager = GridLayoutManager(requireContext(), LogrosAdapter.COLUMNAS)
        }

        perfilViewModel.panel.observe(viewLifecycleOwner) { panel ->
            val conseguidos = panel.logros.count { it.desbloqueado }
            view.findViewById<TextView>(R.id.logrosResumen).text = "$conseguidos de ${panel.logros.size} conseguidos"
            view.findViewById<LinearProgressIndicator>(R.id.logrosBarra).apply {
                max = panel.logros.size
                setProgressCompat(conseguidos, true)
            }
            adapter.setData(panel.logros)
        }
        return view
    }
}
