package com.softcg.myapplication.domain.gamificacion

/** Cada EduCoin ganado es 1 XP. Gastar monedas nunca baja de nivel. */
data class NivelInfo(val numero: Int, val nombre: String, val xpDesde: Int, val recompensa: Int)

/** Dónde está el usuario dentro de la escala de niveles. */
data class ProgresoNivel(val nivel: NivelInfo, val siguiente: NivelInfo?, val xp: Int) {
    val xpEnNivel: Int get() = xp - nivel.xpDesde
    val xpDelNivel: Int get() = siguiente?.let { it.xpDesde - nivel.xpDesde } ?: 0
    val xpRestante: Int get() = siguiente?.let { it.xpDesde - xp } ?: 0
    val fraccion: Float get() = if (siguiente == null) 1f else xpEnNivel.toFloat() / xpDelNivel
}

/**
 * Única fuente de la escala de niveles. Con actividad regular (unos 150 XP por semana) el último
 * nivel llega al terminar un semestre; la recompensa de cada nivel solo se paga una vez.
 */
object NivelesConfig {

    val niveles = listOf(
        NivelInfo(1, "Primíparo", 0, 0),
        NivelInfo(2, "Organizado", 60, 20),
        NivelInfo(3, "Constante", 150, 30),
        NivelInfo(4, "Enfocado", 280, 40),
        NivelInfo(5, "Estratega", 460, 50),
        NivelInfo(6, "Destacado", 700, 60),
        NivelInfo(7, "Referente", 1000, 70),
        NivelInfo(8, "Mentor", 1400, 80),
        NivelInfo(9, "Cum Laude", 1900, 90),
        NivelInfo(10, "Summa Cum Laude", 2500, 100)
    )

    fun progreso(xp: Int): ProgresoNivel {
        val indice = niveles.indexOfLast { xp >= it.xpDesde }.coerceAtLeast(0)
        return ProgresoNivel(niveles[indice], niveles.getOrNull(indice + 1), xp.coerceAtLeast(0))
    }

    fun clave(numero: Int) = "$TIPO_NIVEL:$numero"
}
