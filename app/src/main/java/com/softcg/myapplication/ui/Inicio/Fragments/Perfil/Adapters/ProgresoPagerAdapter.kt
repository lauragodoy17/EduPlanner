package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Tabs.AvatarTabFragment
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Tabs.EstadisticasTabFragment
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Tabs.LogrosTabFragment
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Tabs.MisionesTabFragment

class ProgresoPagerAdapter(padre: Fragment) : FragmentStateAdapter(padre) {

    override fun getItemCount() = TITULOS.size

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> MisionesTabFragment()
        1 -> LogrosTabFragment()
        2 -> EstadisticasTabFragment()
        else -> AvatarTabFragment()
    }

    companion object {
        val TITULOS = listOf("Misiones", "Logros", "Estadísticas", "Avatar")
    }
}
