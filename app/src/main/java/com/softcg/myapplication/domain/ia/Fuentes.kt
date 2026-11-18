package com.softcg.myapplication.domain.ia

enum class TipoFuente { LIBRO, WEB }

/** Lo que Gemini propone como bibliografía. No se muestra hasta comprobar que existe. */
data class FuentePropuesta(
    val tipo: TipoFuente,
    val titulo: String,
    val autor: String,
    val url: String
)

/** Una fuente comprobada: título, autores y enlace salen de la fuente misma, no de lo que escribió Gemini. */
data class Fuente(
    val tipo: TipoFuente,
    val titulo: String,
    /** Autores y año para un libro; el dominio para una página web. */
    val detalle: String,
    val url: String
)

/**
 * Comprueba la bibliografía que propone Gemini: los libros contra un catálogo bibliográfico y las páginas
 * abriéndolas. Lo que no se pueda comprobar se descarta, así que la lista puede volver vacía.
 */
interface VerificadorFuentes {
    suspend fun verificar(propuestas: List<FuentePropuesta>): List<Fuente>
}
