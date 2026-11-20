package com.softcg.myapplication.domain

/**
 * Límites y limpieza de lo que escribe el estudiante, aplicados al guardar (el `maxLength` de los campos solo
 * frena lo que se teclea, no lo que se pega). Un nombre de asignatura también es la clave con la que tareas,
 * notas y sesiones la buscan: por eso nunca lleva saltos de línea, caracteres de control ni espacios de más.
 */
object TextoUsuario {

    const val MAX_TITULO = 80
    const val MAX_NOMBRE_ASIGNATURA = 60
    const val MAX_PERSONA = 60
    const val MAX_DESCRIPCION = 200

    /** Caracteres de control y separadores de línea, salvo el salto de línea: separan palabras. */
    private val CONTROL = Regex("[\\p{Cc}\\u2028\\u2029&&[^\\n]]")

    /** Caracteres de formato invisibles (ancho cero, marcas de dirección): se borran sin dejar espacio. */
    private val INVISIBLES = Regex("\\p{Cf}")

    /** Una sola línea: sin saltos ni caracteres invisibles, espacios colapsados, recortada a [max]. */
    fun linea(texto: String, max: Int): String =
        texto.replace(INVISIBLES, "").replace('\n', ' ').replace(CONTROL, " ")
            .replace(Regex("\\s+"), " ").trim().take(max).trim()

    /** Varias líneas: sin caracteres invisibles, con a lo sumo una línea en blanco seguida, recortada a [max]. */
    fun parrafo(texto: String, max: Int): String =
        texto.replace("\r\n", "\n").replace(INVISIBLES, "").replace(CONTROL, "")
            .lines().joinToString("\n") { it.replace(Regex("[ \\t]+"), " ").trim() }
            .replace(Regex("\n{3,}"), "\n\n").trim().take(max).trim()
}
