package com.softcg.myapplication.ui.Inicio.Fragments.Perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.mostrarMensaje
import com.softcg.myapplication.domain.gamificacion.Accesorio
import com.softcg.myapplication.domain.gamificacion.AccesoriosCatalogo
import com.softcg.myapplication.domain.gamificacion.AvatarBase
import com.softcg.myapplication.domain.gamificacion.LogrosCatalogo
import com.softcg.myapplication.domain.gamificacion.NivelesConfig
import com.softcg.myapplication.domain.gamificacion.ResultadoCompra
import com.softcg.myapplication.domain.gamificacion.ResultadoEquipar
import com.softcg.myapplication.ui.Inicio.Dialogs.ConfirmarCompraDialog
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters.AccesoriosAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TiendaFragment : Fragment() {

    private val perfilViewModel: PerfilViewModel by viewModels()

    private val nivelActual: Int get() = perfilViewModel.panel.value?.nivel?.nivel?.numero ?: 1
    private val base: AvatarBase get() = perfilViewModel.avatarBase.value ?: AvatarBase(0, 0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tienda, container, false)
        initCabecera(view)
        initCatalogo(view)
        initResultados()
        return view
    }

    private fun initCabecera(view: View) {
        val avatar = view.findViewById<AvatarView>(R.id.tiendaAvatar)
        val cambios = MediatorLiveData<Unit>().apply {
            addSource(perfilViewModel.avatarBase) { value = Unit }
            addSource(perfilViewModel.equipados) { value = Unit }
        }
        cambios.observe(viewLifecycleOwner) {
            avatar.mostrar(base, perfilViewModel.equipados.value.orEmpty())
        }
        perfilViewModel.saldo.observe(viewLifecycleOwner) { saldo ->
            view.findViewById<TextView>(R.id.tiendaSaldo).text = "$saldo EduCoins disponibles"
        }
    }

    private fun initCatalogo(view: View) {
        val adapter = AccesoriosAdapter(modoTienda = true) { accesorio -> onAccesorioSeleccionado(accesorio) }
        val recyclerView = view.findViewById<RecyclerView>(R.id.tiendaCatalogo)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(requireContext(), AccesoriosAdapter.COLUMNAS).apply {
            spanSizeLookup = adapter.spanSizeLookup
        }

        val cambios = MediatorLiveData<Unit>().apply {
            addSource(perfilViewModel.saldo) { value = Unit }
            addSource(perfilViewModel.inventario) { value = Unit }
            addSource(perfilViewModel.equipados) { value = Unit }
            addSource(perfilViewModel.avatarBase) { value = Unit }
            addSource(perfilViewModel.panel) { value = Unit }
        }
        cambios.observe(viewLifecycleOwner) {
            adapter.setData(
                AccesoriosCatalogo.accesorios,
                perfilViewModel.inventario.value.orEmpty(),
                perfilViewModel.equipados.value.orEmpty().values.toSet(),
                perfilViewModel.saldo.value ?: 0,
                base,
                nivelActual
            )
        }
    }

    private fun onAccesorioSeleccionado(accesorio: Accesorio) {
        val equipados = perfilViewModel.equipados.value.orEmpty()
        when {
            equipados[accesorio.categoria] == accesorio.id -> perfilViewModel.retirar(accesorio.categoria)
            accesorio.id in perfilViewModel.inventario.value.orEmpty() -> perfilViewModel.equipar(accesorio.id)
            accesorio.logroRequerido != null -> requireActivity().mostrarMensaje(
                "Se gana con el logro «${LogrosCatalogo.buscar(accesorio.logroRequerido)?.titulo.orEmpty()}»"
            )
            nivelActual < accesorio.nivelMinimo -> requireActivity().mostrarMensaje(mensajeNivel(accesorio.nivelMinimo))
            else -> ConfirmarCompraDialog(
                context = requireContext(),
                accesorio = accesorio,
                saldo = perfilViewModel.saldo.value ?: 0,
                avatarBase = base,
                equipados = equipados,
                onComprar = { perfilViewModel.comprar(accesorio.id) }
            ).show()
        }
    }

    private fun mensajeNivel(nivel: Int): String {
        val nombre = NivelesConfig.niveles.firstOrNull { it.numero == nivel }?.nombre.orEmpty()
        return "Disponible desde el nivel $nivel ($nombre)"
    }

    private fun initResultados() {
        perfilViewModel.resultadoCompra.observe(viewLifecycleOwner) { evento ->
            val mensaje = when (val resultado = evento.getContentIfNotHandled() ?: return@observe) {
                is ResultadoCompra.Comprado ->
                    "¡Compraste ${resultado.accesorio.nombre}! Ya está en tu inventario; tócalo para ponérselo a tu avatar"
                is ResultadoCompra.SaldoInsuficiente ->
                    "Necesitas ${resultado.precio} EduCoins y tienes ${resultado.saldo}"
                is ResultadoCompra.NivelInsuficiente -> mensajeNivel(resultado.nivelMinimo)
                ResultadoCompra.SoloPorLogro -> "Este accesorio no se vende: se gana con un logro"
                ResultadoCompra.YaLoTiene -> "Ya tienes este accesorio"
                ResultadoCompra.NoExiste -> "Este accesorio ya no está disponible"
                ResultadoCompra.SinSesion, ResultadoCompra.Error ->
                    "No se pudo completar la compra. No se descontaron EduCoins"
            }
            requireActivity().mostrarMensaje(mensaje)
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
