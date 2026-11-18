package com.softcg.myapplication.core

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Release: Play Integrity. La huella SHA-256 de la firma debe estar registrada en Firebase > App Check. */
object AppCheckProveedor {
    fun fabrica(): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
}
