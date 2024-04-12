package com.softcg.myapplication.core.ex


import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.softcg.myapplication.core.dialog.DialogFragmentLauncher

fun DialogFragment.show(launcher: DialogFragmentLauncher, activity: FragmentActivity) {
    launcher.show(this, activity)
}