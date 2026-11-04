package com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.Accesorio
import com.softcg.myapplication.domain.gamificacion.AvatarBase
import com.softcg.myapplication.domain.gamificacion.CategoriaAccesorio
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.AvatarView

/**
 * Accesorios agrupados por categoría. En la tienda muestra precio y si se puede comprar;
 * en el inventario solo los que el usuario posee y si están equipados.
 */
class AccesoriosAdapter(
    private val modoTienda: Boolean,
    private val onClick: (Accesorio) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIPO_CATEGORIA = 0
        private const val TIPO_ACCESORIO = 1
        const val COLUMNAS = 3
    }

    private sealed class Fila {
        data class Categoria(val categoria: CategoriaAccesorio) : Fila()
        data class Item(val accesorio: Accesorio) : Fila()
    }

    private var filas = emptyList<Fila>()
    private var inventario = emptySet<String>()
    private var equipados = emptySet<String>()
    private var saldo = 0
    private var nivel = 1
    private var avatarBase = AvatarBase(0, 0)

    fun setData(
        accesorios: List<Accesorio>,
        inventario: Set<String>,
        equipados: Set<String>,
        saldo: Int,
        avatarBase: AvatarBase,
        nivel: Int = 1
    ) {
        this.inventario = inventario
        this.equipados = equipados
        this.saldo = saldo
        this.avatarBase = avatarBase
        this.nivel = nivel
        filas = accesorios.groupBy { it.categoria }
            .toSortedMap()
            .flatMap { (categoria, lista) -> listOf(Fila.Categoria(categoria)) + lista.map { Fila.Item(it) } }
        notifyDataSetChanged()
    }

    /** Las cabeceras de categoría ocupan toda la fila de la cuadrícula. */
    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int) =
            if (filas.getOrNull(position) is Fila.Categoria) COLUMNAS else 1
    }

    override fun getItemViewType(position: Int) =
        if (filas[position] is Fila.Categoria) TIPO_CATEGORIA else TIPO_ACCESORIO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_CATEGORIA) {
            CategoriaViewHolder(inflater.inflate(R.layout.item_categoria_accesorio, parent, false))
        } else {
            AccesorioViewHolder(inflater.inflate(R.layout.item_accesorio, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val fila = filas[position]) {
            is Fila.Categoria -> (holder as CategoriaViewHolder).bind(fila.categoria)
            is Fila.Item -> (holder as AccesorioViewHolder).bind(fila.accesorio)
        }
    }

    override fun getItemCount() = filas.size

    class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombre: TextView = itemView.findViewById(R.id.categoriaNombre)
        fun bind(categoria: CategoriaAccesorio) {
            nombre.text = categoria.nombre
        }
    }

    inner class AccesorioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: AvatarView = itemView.findViewById(R.id.accesorioAvatar)
        private val nombre: TextView = itemView.findViewById(R.id.accesorioNombre)
        private val precioFila: View = itemView.findViewById(R.id.accesorioPrecioFila)
        private val precio: TextView = itemView.findViewById(R.id.accesorioPrecio)
        private val estado: TextView = itemView.findViewById(R.id.accesorioEstado)

        fun bind(accesorio: Accesorio) {
            val context = itemView.context
            avatar.mostrar(avatarBase, mapOf(accesorio.categoria to accesorio.id))
            nombre.text = accesorio.nombre
            precio.text = accesorio.precio.toString()
            // Los exclusivos no se venden: no tienen precio que mostrar
            precioFila.visibility = if (modoTienda && accesorio.logroRequerido == null) View.VISIBLE else View.GONE

            val (texto, color) = when {
                accesorio.id in equipados -> "EQUIPADO" to context.getColor(R.color.purple_primary)
                accesorio.id in inventario -> (if (modoTienda) "EN INVENTARIO" else "EQUIPAR") to context.getColor(R.color.purple_light)
                accesorio.logroRequerido != null -> "POR LOGRO" to context.getColor(R.color.logro_oro)
                nivel < accesorio.nivelMinimo -> "NIVEL ${accesorio.nivelMinimo}" to context.getColor(R.color.gris)
                saldo >= accesorio.precio -> "COMPRAR" to context.getColor(R.color.purple_accent)
                else -> "FALTAN ${accesorio.precio - saldo}" to context.getColor(R.color.gris)
            }
            estado.text = texto
            estado.backgroundTintList = android.content.res.ColorStateList.valueOf(color)

            itemView.contentDescription = buildString {
                append(accesorio.nombre)
                if (modoTienda && accesorio.logroRequerido == null) append(", ${accesorio.precio} EduCoins")
                append(", ").append(texto.lowercase())
            }
            itemView.setOnClickListener { onClick(accesorio) }
        }
    }
}
