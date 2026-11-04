package com.softcg.myapplication.ui.Inicio.Dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.softcg.myapplication.R
import com.softcg.myapplication.core.ex.showAsBottomSheet
import com.softcg.myapplication.domain.gamificacion.Accesorio
import com.softcg.myapplication.domain.gamificacion.AvatarBase
import com.softcg.myapplication.domain.gamificacion.CategoriaAccesorio
import com.softcg.myapplication.ui.Inicio.Fragments.Perfil.AvatarView

/**
 * Muestra el accesorio puesto en el avatar, su precio y el saldo que quedará antes de comprar.
 * [saldo] solo se usa para informar: la compra vuelve a validar el saldo al guardarse.
 */
class ConfirmarCompraDialog(
    private val context: Context,
    private val accesorio: Accesorio,
    private val saldo: Int,
    private val avatarBase: AvatarBase,
    private val equipados: Map<CategoriaAccesorio, String>,
    private val onComprar: () -> Unit
) {

    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sheet_confirmar_compra)

        dialog.findViewById<AvatarView>(R.id.compraAvatar)
            .mostrar(avatarBase, equipados + (accesorio.categoria to accesorio.id))
        dialog.findViewById<TextView>(R.id.compraNombre).text = accesorio.nombre
        dialog.findViewById<TextView>(R.id.compraPrecio).text = "${accesorio.precio} EduCoins"

        val titulo = dialog.findViewById<TextView>(R.id.compraTitulo)
        val resumen = dialog.findViewById<TextView>(R.id.compraSaldo)
        val comprar = dialog.findViewById<Button>(R.id.botonComprar)

        if (saldo >= accesorio.precio) {
            resumen.text = "Tu saldo: $saldo EduCoins · Te quedarán: ${saldo - accesorio.precio}"
            comprar.text = "Comprar por ${accesorio.precio} EduCoins"
            comprar.setOnClickListener {
                dialog.dismiss()
                onComprar()
            }
        } else {
            titulo.text = "EduCoins insuficientes"
            resumen.text = "Este accesorio cuesta ${accesorio.precio} EduCoins y tienes $saldo.\n" +
                "Te faltan ${accesorio.precio - saldo}: completa misiones y sesiones de enfoque para ganarlos."
            comprar.text = "Entendido"
            comprar.setOnClickListener { dialog.dismiss() }
        }

        dialog.findViewById<Button>(R.id.botonCancelarCompra).setOnClickListener { dialog.dismiss() }
        dialog.showAsBottomSheet()
    }
}
