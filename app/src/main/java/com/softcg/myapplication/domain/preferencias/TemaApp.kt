package com.softcg.myapplication.domain.preferencias

/**
 * Temas que el estudiante puede elegir en Opciones. [clave] se guarda en las preferencias: no cambiarla.
 * El inicio de sesión siempre se ve en [CLARO]: aún no hay un estudiante con preferencias.
 */
enum class TemaApp(val clave: String, val nombre: String, val descripcion: String, val oscuro: Boolean = false) {
    CLARO("claro", "Claro", "El original: fondos claros y morado"),
    OSCURO("oscuro", "Oscuro", "Fondos oscuros, cómodo de noche", oscuro = true),
    AZUL("azul", "Azul", "Paleta de tonos azules"),
    CLASICO("clasico", "Clásico", "Paleta de tonos verdes");

    companion object {
        val PREDETERMINADO = CLARO

        /** Una clave desconocida (o de una versión anterior) vuelve al tema predeterminado. */
        fun deClave(clave: String?): TemaApp = entries.firstOrNull { it.clave == clave } ?: PREDETERMINADO
    }
}
