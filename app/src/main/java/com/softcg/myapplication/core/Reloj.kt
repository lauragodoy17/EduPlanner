package com.softcg.myapplication.core

import java.time.ZonedDateTime
import javax.inject.Inject

/** Hora actual en la zona del dispositivo; se sustituye en los tests. */
interface Reloj {
    fun ahora(): ZonedDateTime
}

class RelojSistema @Inject constructor() : Reloj {
    override fun ahora(): ZonedDateTime = ZonedDateTime.now()
}
