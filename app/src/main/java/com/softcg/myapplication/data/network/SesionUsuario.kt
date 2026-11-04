package com.softcg.myapplication.data.network

import javax.inject.Inject

/** Usuario con sesión iniciada. EduCoins, asistencias, inventario y avatar se guardan por uid. */
interface SesionUsuario {
    fun uid(): String?
    fun email(): String?
}

class FirebaseSesionUsuario @Inject constructor(private val firebase: FirebaseClient) : SesionUsuario {
    override fun uid(): String? = firebase.auth.currentUser?.uid
    override fun email(): String? = firebase.auth.currentUser?.email
}
