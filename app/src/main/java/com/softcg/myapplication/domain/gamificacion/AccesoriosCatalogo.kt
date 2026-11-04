package com.softcg.myapplication.domain.gamificacion

import kotlin.math.abs

/** Orden de las categorías = orden de las capas del avatar, de atrás hacia delante. */
enum class CategoriaAccesorio(val nombre: String) {
    FONDO("Fondos"),
    MOCHILA("Mochilas"),
    ACADEMICO("Académicos"),
    GAFAS("Gafas"),
    CABEZA("Cabeza")
}

/**
 * [nivelMinimo]: el nivel desde el que se puede comprar.
 * [logroRequerido]: no se vende; se recibe gratis al conseguir ese logro.
 */
data class Accesorio(
    val id: String,
    val nombre: String,
    val categoria: CategoriaAccesorio,
    val precio: Int,
    val nivelMinimo: Int = 1,
    val logroRequerido: String? = null
)

/** Fuente confiable de los precios: la tienda solo envía el id del accesorio. */
object AccesoriosCatalogo {

    val accesorios = listOf(
        Accesorio("fondo_estrellas", "Cielo estrellado", CategoriaAccesorio.FONDO, 40),
        Accesorio("fondo_biblioteca", "Biblioteca", CategoriaAccesorio.FONDO, 60),
        Accesorio("fondo_atardecer", "Atardecer", CategoriaAccesorio.FONDO, 80),
        Accesorio("fondo_aurora", "Aurora", CategoriaAccesorio.FONDO, 120, nivelMinimo = 5),
        Accesorio("fondo_laureles", "Salón de honor", CategoriaAccesorio.FONDO, 0, logroRequerido = "misiones_60"),
        Accesorio("mochila_escolar", "Mochila escolar", CategoriaAccesorio.MOCHILA, 80),
        Accesorio("libro", "Libro", CategoriaAccesorio.ACADEMICO, 30),
        Accesorio("medalla", "Medalla de honor", CategoriaAccesorio.ACADEMICO, 120, nivelMinimo = 6),
        Accesorio("gafas_redondas", "Gafas redondas", CategoriaAccesorio.GAFAS, 40),
        Accesorio("gafas_sol", "Gafas de sol", CategoriaAccesorio.GAFAS, 60),
        Accesorio("gafas_doradas", "Gafas doradas", CategoriaAccesorio.GAFAS, 0, logroRequerido = "sin_procrastinar"),
        Accesorio("gorra", "Gorra", CategoriaAccesorio.CABEZA, 50),
        Accesorio("gorro_lana", "Gorro de lana", CategoriaAccesorio.CABEZA, 70),
        Accesorio("audifonos", "Audífonos", CategoriaAccesorio.CABEZA, 90, nivelMinimo = 3),
        Accesorio("birrete", "Birrete de graduación", CategoriaAccesorio.CABEZA, 150, nivelMinimo = 9),
        Accesorio("corona_laurel", "Corona de laurel", CategoriaAccesorio.CABEZA, 0, logroRequerido = "racha_21")
    )

    fun buscar(id: String): Accesorio? = accesorios.firstOrNull { it.id == id }

    fun exclusivoDeLogro(logroId: String): Accesorio? = accesorios.firstOrNull { it.logroRequerido == logroId }
}

/** Apariencia base del avatar: la que eligió el usuario o, si no eligió, siempre la misma para su uid. */
data class AvatarBase(val indicePiel: Int, val indicePelo: Int) {
    companion object {
        const val TONOS = 5

        fun desdeUid(uid: String): AvatarBase {
            val hash = abs(uid.hashCode().toLong())
            return AvatarBase((hash % TONOS).toInt(), ((hash / TONOS) % TONOS).toInt())
        }
    }
}
