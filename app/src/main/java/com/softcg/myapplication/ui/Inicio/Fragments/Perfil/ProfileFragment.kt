package com.softcg.myapplication.ui.Inicio.Fragments.Perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.AvatarBase
import com.softcg.myapplication.domain.gamificacion.PanelProgreso
import com.softcg.myapplication.ui.Inicio.Dialogs.RutaNivelesDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters.ProgresoPagerAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Views.AnilloProgresoView
import dagger.hilt.android.AndroidEntryPoint

/** Centro de progreso: cabecera con nivel, racha y saldo, y una pestaña por cada tema. */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val perfilViewModel: PerfilViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        initCabecera(view)
        initPestanas(view, savedInstanceState)
        return view
    }

    override fun onResume() {
        super.onResume()
        perfilViewModel.refrescar()
    }

    private fun initCabecera(view: View) {
        val anillo = view.findViewById<AnilloProgresoView>(R.id.perfilAnillo)
        anillo.grosor(5f)
        anillo.colores(requireContext().getColor(R.color.blanco_25), requireContext().getColor(R.color.xp_dorado))

        val avatar = view.findViewById<AvatarView>(R.id.perfilAvatar)
        val cambiosAvatar = MediatorLiveData<Unit>().apply {
            addSource(perfilViewModel.avatarBase) { value = Unit }
            addSource(perfilViewModel.equipados) { value = Unit }
        }
        cambiosAvatar.observe(viewLifecycleOwner) {
            avatar.mostrar(
                perfilViewModel.avatarBase.value ?: AvatarBase(0, 0),
                perfilViewModel.equipados.value.orEmpty()
            )
        }

        view.findViewById<TextView>(R.id.perfilNombre).text = perfilViewModel.email.substringBefore("@")
        perfilViewModel.saldo.observe(viewLifecycleOwner) { saldo ->
            view.findViewById<TextView>(R.id.perfilSaldo).text = saldo.toString()
        }
        perfilViewModel.panel.observe(viewLifecycleOwner) { panel -> mostrarPanel(view, anillo, panel) }

        view.findViewById<View>(R.id.perfilHero).setOnClickListener {
            perfilViewModel.panel.value?.let { RutaNivelesDialog(requireContext(), it.nivel).show() }
        }
    }

    private fun mostrarPanel(view: View, anillo: AnilloProgresoView, panel: PanelProgreso) {
        val progreso = panel.nivel
        view.findViewById<TextView>(R.id.perfilNivelEtiqueta).text = "NIVEL ${progreso.nivel.numero}"
        view.findViewById<TextView>(R.id.perfilNivelNombre).text = progreso.nivel.nombre
        view.findViewById<TextView>(R.id.perfilXp).text = progreso.siguiente?.let {
            "Faltan ${progreso.xpRestante} XP para ${it.nombre}"
        } ?: "${progreso.xp} XP · Nivel máximo"
        anillo.fijar(progreso.fraccion)
        anillo.contentDescription = "Progreso del nivel: ${(progreso.fraccion * 100).toInt()} %"

        view.findViewById<TextView>(R.id.perfilRacha).text = panel.racha.actual.toString()
        view.findViewById<TextView>(R.id.perfilLogros).text =
            "${panel.logros.count { it.desbloqueado }}/${panel.logros.size}"
    }

    private fun initPestanas(view: View, savedInstanceState: Bundle?) {
        val pager = view.findViewById<ViewPager2>(R.id.perfilPager)
        pager.adapter = ProgresoPagerAdapter(this)
        TabLayoutMediator(view.findViewById<TabLayout>(R.id.perfilTabs), pager) { pestana, posicion ->
            pestana.text = ProgresoPagerAdapter.TITULOS[posicion]
        }.attach()

        if (savedInstanceState == null) {
            pager.setCurrentItem(arguments?.getInt(ARG_PESTANA) ?: 0, false)
        }
    }

    companion object {
        const val ARG_PESTANA = "pestana"
        const val PESTANA_LOGROS = 1
    }
}
