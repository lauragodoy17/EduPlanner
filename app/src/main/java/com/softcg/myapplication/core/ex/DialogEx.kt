package com.softcg.myapplication.core.ex


import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.softcg.myapplication.R
import com.softcg.myapplication.core.dialog.DialogFragmentLauncher
import kotlin.math.min

fun DialogFragment.show(launcher: DialogFragmentLauncher, activity: FragmentActivity) {
    launcher.show(this, activity)
}

/** Muestra el diálogo como hoja inferior a pantalla completa con la animación de la app. */
fun Dialog.showAsBottomSheet() {
    show()
    // En tabletas y en horizontal no ocupa todo el ancho
    val recursos = context.resources
    val ancho = min(recursos.displayMetrics.widthPixels, recursos.getDimensionPixelSize(R.dimen.ancho_max_hoja))
    window?.setLayout(ancho, ViewGroup.LayoutParams.WRAP_CONTENT)
    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    window?.attributes?.windowAnimations = R.style.DialogAnimation
    window?.setGravity(Gravity.BOTTOM)
}
