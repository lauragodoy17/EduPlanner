package com.softcg.myapplication.core

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug: el primer arranque escribe en Logcat ("DebugAppCheckProvider") un token que hay que añadir en
 * Firebase > App Check > Apps > Administrar tokens de depuración. Cada emulador o instalación limpia genera uno nuevo.
 */
object AppCheckProveedor {
    fun fabrica(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
