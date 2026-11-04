package com.softcg.myapplication.ui.Inicio.Fragments.Perfil

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.softcg.myapplication.R
import com.softcg.myapplication.domain.gamificacion.AvatarBase
import com.softcg.myapplication.domain.gamificacion.CategoriaAccesorio
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.Models.AccesorioRecursos

/**
 * Avatar circular compuesto por capas: fondo, mochila, personaje base y accesorios.
 * Las capas comparten el mismo lienzo, así que cada accesorio cae en su sitio.
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        setBackgroundColor(context.getColor(R.color.purple_surface))
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        clipToOutline = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "Avatar"
    }

    /** [equipados]: categoría → id del accesorio que lleva puesto. */
    fun mostrar(base: AvatarBase, equipados: Map<CategoriaAccesorio, String>) {
        removeAllViews()

        equipados[CategoriaAccesorio.FONDO]?.let { agregarAccesorio(it) }
        equipados[CategoriaAccesorio.MOCHILA]?.let { agregarAccesorio(it) }

        agregarCapa(R.drawable.avatar_cuerpo)
        agregarCapa(R.drawable.avatar_piel, AccesorioRecursos.TONOS_PIEL[base.indicePiel])
        agregarCapa(R.drawable.avatar_pelo, AccesorioRecursos.TONOS_PELO[base.indicePelo])
        agregarCapa(R.drawable.avatar_cara)

        equipados[CategoriaAccesorio.MOCHILA]?.let { id -> AccesorioRecursos.dibujoFrontal(id)?.let { agregarCapa(it) } }
        equipados[CategoriaAccesorio.ACADEMICO]?.let { agregarAccesorio(it) }
        equipados[CategoriaAccesorio.GAFAS]?.let { agregarAccesorio(it) }
        equipados[CategoriaAccesorio.CABEZA]?.let { agregarAccesorio(it) }

        contentDescription = if (equipados.isEmpty()) "Avatar sin accesorios" else "Avatar con ${equipados.size} accesorios"
    }

    private fun agregarAccesorio(itemId: String) {
        AccesorioRecursos.dibujo(itemId)?.let { agregarCapa(it) }
    }

    private fun agregarCapa(@DrawableRes dibujo: Int, @ColorInt tinte: Int? = null) {
        val capa = ImageView(context).apply {
            setImageResource(dibujo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            tinte?.let { imageTintList = ColorStateList.valueOf(it) }
        }
        addView(capa, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}
