package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Tabs

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.domain.gamificacion.AccesoriosCatalogo
import com.softcg.myapplication.domain.gamificacion.AvatarBase
import com.softcg.myapplication.domain.gamificacion.ResultadoEquipar
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters.AccesoriosAdapter
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.AvatarView
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Models.AccesorioRecursos
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.PerfilViewModel

/** Personalización: tonos del avatar, accesorios propios y acceso a la tienda. */
class AvatarTabFragment : Fragment() {

    private val perfilViewModel: PerfilViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tab_avatar, container, false)
        initVistaPrevia(view)
        initInventario(view)
        view.findViewById<Button>(R.id.botonTienda).setOnClickListener {
            findNavController().navigate(R.id.id_shop_fragment)
        }
        return view
    }

    private fun base() = perfilViewModel.avatarBase.value ?: AvatarBase(0, 0)

    private fun initVistaPrevia(view: View) {
        val avatar = view.findViewById<AvatarView>(R.id.avatarVistaPrevia)
        val tonosPiel = view.findViewById<LinearLayout>(R.id.avatarTonosPiel)
        val tonosPelo = view.findViewById<LinearLayout>(R.id.avatarTonosPelo)

        val cambios = MediatorLiveData<Unit>().apply {
            addSource(perfilViewModel.avatarBase) { value = Unit }
            addSource(perfilViewModel.equipados) { value = Unit }
        }
        cambios.observe(viewLifecycleOwner) {
            val actual = base()
            avatar.mostrar(actual, perfilViewModel.equipados.value.orEmpty())
            pintarTonos(tonosPiel, AccesorioRecursos.TONOS_PIEL, actual.indicePiel, "Tono de piel") {
                perfilViewModel.elegirApariencia(base().copy(indicePiel = it))
            }
            pintarTonos(tonosPelo, AccesorioRecursos.TONOS_PELO, actual.indicePelo, "Color de pelo") {
                perfilViewModel.elegirApariencia(base().copy(indicePelo = it))
            }
        }

        perfilViewModel.aparienciaGuardada.observe(viewLifecycleOwner) { evento ->
            if (evento.getContentIfNotHandled() == false) {
                requireActivity().mostrarMensaje("No se pudo actualizar tu avatar. Inténtalo de nuevo")
            }
        }
    }

    private fun pintarTonos(
        contenedor: LinearLayout,
        tonos: List<Int>,
        seleccionado: Int,
        descripcion: String,
        onElegir: (Int) -> Unit
    ) {
        contenedor.removeAllViews()
        val densidad = resources.displayMetrics.density
        tonos.forEachIndexed { indice, color ->
            val elegido = indice == seleccionado
            val tono = ImageView(requireContext()).apply {
                setBackgroundResource(R.drawable.circle_shape)
                backgroundTintList = ColorStateList.valueOf(color)
                if (elegido) {
                    setImageResource(R.drawable.ic_gami_check)
                    imageTintList = ColorStateList.valueOf(context.getColor(R.color.white))
                    setPadding((8 * densidad).toInt(), (8 * densidad).toInt(), (8 * densidad).toInt(), (8 * densidad).toInt())
                }
                contentDescription = "$descripcion ${indice + 1}" + if (elegido) ", elegido" else ""
                setOnClickListener { if (!elegido) onElegir(indice) }
            }
            contenedor.addView(tono, LinearLayout.LayoutParams((40 * densidad).toInt(), (40 * densidad).toInt()).apply {
                marginEnd = (10 * densidad).toInt()
            })
        }
    }

    private fun initInventario(view: View) {
        val adapter = AccesoriosAdapter(modoTienda = false) { accesorio ->
            val equipados = perfilViewModel.equipados.value.orEmpty()
            if (equipados[accesorio.categoria] == accesorio.id) {
                perfilViewModel.retirar(accesorio.categoria)
            } else {
                perfilViewModel.equipar(accesorio.id)
            }
        }
        view.findViewById<RecyclerView>(R.id.perfilInventario).apply {
            this.adapter = adapter
            layoutManager = GridLayoutManager(requireContext(), AccesoriosAdapter.COLUMNAS).apply {
                spanSizeLookup = adapter.spanSizeLookup
            }
        }

        // Se redibuja cuando cambia el inventario o lo que lleva puesto
        val cambios = MediatorLiveData<Unit>().apply {
            addSource(perfilViewModel.inventario) { value = Unit }
            addSource(perfilViewModel.equipados) { value = Unit }
            addSource(perfilViewModel.avatarBase) { value = Unit }
        }
        cambios.observe(viewLifecycleOwner) {
            val inventario = perfilViewModel.inventario.value.orEmpty()
            val equipados = perfilViewModel.equipados.value.orEmpty().values.toSet()
            val propios = AccesoriosCatalogo.accesorios.filter { it.id in inventario }
            adapter.setData(propios, inventario, equipados, 0, base())
            view.findViewById<View>(R.id.perfilInventarioVacio).visibility =
                if (propios.isEmpty()) View.VISIBLE else View.GONE
        }

        perfilViewModel.resultadoEquipar.observe(viewLifecycleOwner) { evento ->
            when (evento.getContentIfNotHandled()) {
                ResultadoEquipar.NoLoPosee -> requireActivity().mostrarMensaje("Solo puedes equipar accesorios que hayas comprado")
                ResultadoEquipar.Error, ResultadoEquipar.SinSesion, ResultadoEquipar.NoExiste ->
                    requireActivity().mostrarMensaje("No se pudo actualizar tu avatar. Inténtalo de nuevo")
                else -> {}
            }
        }
    }
}
