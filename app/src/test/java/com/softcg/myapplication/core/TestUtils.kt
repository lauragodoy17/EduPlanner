package com.softcg.myapplication.core

import com.softcg.myapplication.data.Repositories.GamificacionRepository
import io.mockk.coEvery
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

val ZONA_PRUEBAS: ZoneId = ZoneId.of("America/Bogota")

/** Reloj controlado por el test. */
class RelojFijo(var ahora: LocalDateTime) : Reloj {
    override fun ahora(): ZonedDateTime = ahora.atZone(ZONA_PRUEBAS)
}

fun LocalDateTime.millis(): Long = atZone(ZONA_PRUEBAS).toInstant().toEpochMilli()

/** En los tests de JVM la "transacción" ejecuta el bloque directamente (Room se prueba en androidTest). */
fun GamificacionRepository.ejecutarTransaccionesDirectamente() {
    coEvery { enTransaccion<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
}
