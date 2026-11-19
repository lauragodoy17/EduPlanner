package com.softcg.myapplication.domain.prediccion

/** Qué decirle al estudiante sobre el área de una asignatura, según lo que se pudo detectar de su nombre. */
object TextosArea {

    /** Cuando no hay área guardada: se le pide al estudiante explicando por qué no se asignó sola. */
    fun pregunta(nombre: String, deteccion: DeteccionArea): String = when (deteccion) {
        is DeteccionArea.Ambigua ->
            "«$nombre» encaja con ${unir(deteccion.candidatas.map(CatalogoModelo::etiqueta))}: elige la correcta"
        // Unica no debería llegar aquí (se asigna sola), pero si llega se ofrece como confirmación
        is DeteccionArea.Unica -> "Confirma el área: ${CatalogoModelo.etiqueta(deteccion.area)}"
        DeteccionArea.Ninguna -> "No pudimos relacionar «$nombre» con un área: elige la que más se parezca"
    }

    /** "A", "A o B", "A, B o C". */
    fun unir(opciones: List<String>): String = when (opciones.size) {
        0 -> ""
        1 -> opciones.single()
        else -> opciones.dropLast(1).joinToString(", ") + " o " + opciones.last()
    }
}
