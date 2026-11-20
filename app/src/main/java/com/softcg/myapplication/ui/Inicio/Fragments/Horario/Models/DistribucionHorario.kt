package com.softcg.myapplication.ui.Inicio.Fragments.Horario.Models

/** Dónde va una clase en el horario: su columna y cuántas columnas tiene su grupo de clases que se cruzan. */
data class Ubicacion(val columna: Int, val columnas: Int)

/**
 * Reparte en columnas las clases que se cruzan, como un calendario: cada grupo de clases que se solapan (directa o
 * encadenadamente) se divide en tantas columnas como clases simultáneas tenga como máximo; una clase sola usa todo el
 * ancho. Dos clases seguidas (una termina cuando empieza la otra) no se cruzan.
 */
object DistribucionHorario {

    /** [intervalos]: inicio y fin de cada clase en minutos del día. Devuelve una ubicación por clase, en el mismo orden. */
    fun distribuir(intervalos: List<Pair<Int, Int>>): List<Ubicacion> {
        val resultado = arrayOfNulls<Ubicacion>(intervalos.size)
        val orden = intervalos.indices.sortedWith(compareBy({ intervalos[it].first }, { -intervalos[it].second }))

        var grupo = mutableListOf<Pair<Int, Int>>() // índice de la clase → columna
        var finDeColumna = mutableListOf<Int>()
        var finDelGrupo = Int.MIN_VALUE

        fun cerrarGrupo() {
            grupo.forEach { (indice, columna) -> resultado[indice] = Ubicacion(columna, finDeColumna.size) }
            grupo = mutableListOf()
            finDeColumna = mutableListOf()
        }

        orden.forEach { indice ->
            val (inicio, finBruto) = intervalos[indice]
            val fin = maxOf(finBruto, inicio + 1)
            if (grupo.isNotEmpty() && inicio >= finDelGrupo) cerrarGrupo()
            // La primera columna que ya quedó libre; si no hay, una nueva
            val libre = finDeColumna.indexOfFirst { it <= inicio }
            val columna = if (libre >= 0) libre else finDeColumna.size.also { finDeColumna.add(0) }
            finDeColumna[columna] = fin
            grupo.add(indice to columna)
            finDelGrupo = if (grupo.size == 1) fin else maxOf(finDelGrupo, fin)
        }
        if (grupo.isNotEmpty()) cerrarGrupo()
        return resultado.map { it ?: Ubicacion(0, 1) }
    }

    /** "08:30" o "8: 5" → minutos del día; null si no es una hora. */
    fun minutos(hora: String): Int? {
        val partes = hora.replace(" ", "").split(":")
        if (partes.size < 2) return null
        val h = partes[0].toIntOrNull() ?: return null
        val m = partes[1].toIntOrNull() ?: return null
        return if (h in 0..23 && m in 0..59) h * 60 + m else null
    }
}
