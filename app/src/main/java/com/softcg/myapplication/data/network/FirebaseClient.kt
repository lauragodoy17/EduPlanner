package com.softcg.myapplication.data.network
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class FirebaseClient @Inject constructor() {

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

}