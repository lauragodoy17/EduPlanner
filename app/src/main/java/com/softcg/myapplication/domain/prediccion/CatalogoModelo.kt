package com.softcg.myapplication.domain.prediccion

import java.text.Normalizer

/**
 * Categorías y rangos con que se entrenó el modelo, copiados de GET /metadata de la API (sklearn 1.6.1).
 * Una categoría que no esté aquí la API la acepta pero la ignora (OneHotEncoder con handle_unknown="ignore")
 * y la predicción empeora, así que la app solo envía valores de este catálogo. Van sin tildes, como en el
 * entrenamiento. Si el modelo se reentrena con otras categorías, hay que actualizar este archivo.
 */
object CatalogoModelo {

    val TEMAS_POR_AREA: Map<String, List<String>> = mapOf(
        "Arte" to listOf("Apreciacion artistica", "Diseno grafico", "Escultura y ceramica", "Historia del arte", "Tecnicas de dibujo"),
        "Biologia" to listOf("Anatomia humana", "Celula y genetica", "Ecosistemas", "Evolucion", "Microbiologia"),
        "Fisica" to listOf("Electromagnetismo", "Fisica moderna", "Mecanica clasica", "Ondas y optica", "Termodinamica"),
        "Geografia" to listOf("Cartografia", "Climatologia", "Geografia fisica", "Geografia humana", "Geopolitica"),
        "Historia" to listOf("Geopolitica mundial", "Historia antigua", "Historia contemporanea", "Historia moderna", "Revoluciones y conflictos"),
        "Ingles" to listOf("Comprension de lectura", "Expresion oral", "Gramatica y sintaxis", "Vocabulario tematico", "Writing avanzado"),
        "Literatura" to listOf("Analisis literario", "Literatura universal", "Narrativa latinoamericana", "Poesia y retorica", "Teatro y dramaturgia"),
        "Matematicas" to listOf("Algebra lineal", "Calculo diferencial", "Integrales", "Probabilidad y estadistica", "Trigonometria"),
        "Programacion" to listOf("Algoritmos y estructuras de datos", "Bases de datos", "Desarrollo web", "Inteligencia artificial", "Programacion orientada a objetos"),
        "Quimica" to listOf("Electroquimica", "Enlace quimico", "Estequiometria", "Quimica organica", "Termodinamica quimica")
    )

    val AREAS: List<String> = TEMAS_POR_AREA.keys.sorted()

    val METODOS: List<String> = listOf(
        "Ejercicios practicos", "Fichas de estudio", "Grupos de estudio", "Lectura activa", "Lectura pasiva",
        "Mapas mentales", "Metodo Feynman", "Pomodoro", "Repaso espaciado", "Sin metodo definido",
        "Subrayado y resumen", "Videos educativos"
    )

    /**
     * Los 8 métodos que el modelo recommended_method puede recomendar (clases de le_method). Son menos que
     * [METODOS] porque estos últimos son los que el estudiante puede declarar que usa.
     */
    val METODOS_RECOMENDABLES: List<String> = listOf(
        "Fichas de estudio", "Grupos de estudio", "Lectura activa", "Mapas mentales",
        "Metodo Feynman", "Pomodoro", "Repaso espaciado", "Subrayado y resumen"
    )

    /** Rangos de los datos de entrenamiento: fuera de ellos el modelo extrapola y la predicción es menos fiable. */
    val RANGO_ASISTENCIA = 0.08..9.09
    val RANGO_INTENSIDAD = 1.0..6.0

    fun temasDe(area: String?): List<String> = TEMAS_POR_AREA[area].orEmpty()

    fun esArea(area: String?) = area != null && area in TEMAS_POR_AREA
    fun esTema(area: String?, tema: String?) = tema != null && tema in temasDe(area)
    fun esMetodo(metodo: String?) = metodo != null && metodo in METODOS

    /** Nombre para mostrar: el catálogo no lleva tildes, la interfaz sí. */
    fun etiqueta(valor: String): String = ETIQUETAS[valor] ?: valor

    /**
     * Área del modelo a partir del nombre que el estudiante le dio a la asignatura. Solo [DeteccionArea.Unica] se
     * asigna sin preguntar ("Cálculo II" → Matematicas); si el nombre encaja con varias áreas ("Historia del arte")
     * o con ninguna, se le pregunta al estudiante.
     */
    fun detectarArea(nombreAsignatura: String): DeteccionArea {
        val nombre = normalizar(nombreAsignatura)
        if (nombre.isBlank()) return DeteccionArea.Ninguna
        AREAS.firstOrNull { normalizar(it) == nombre }?.let { return DeteccionArea.Unica(it) }
        // Por inicio de palabra, para que "arte" no coincida dentro de "departamento"
        val palabrasNombre = nombre.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        fun coincide(clave: String) =
            if (' ' in clave) nombre.contains(clave) else palabrasNombre.any { it.startsWith(clave) }
        val candidatas = PALABRAS_POR_AREA.filterValues { claves -> claves.any(::coincide) }.keys.toList()
        return when (candidatas.size) {
            0 -> DeteccionArea.Ninguna
            1 -> DeteccionArea.Unica(candidatas.single())
            else -> DeteccionArea.Ambigua(candidatas)
        }
    }

    /** El área detectada o, si es ambigua, la más probable (las candidatas van en orden de prioridad). */
    fun sugerirArea(nombreAsignatura: String): String? = when (val deteccion = detectarArea(nombreAsignatura)) {
        is DeteccionArea.Unica -> deteccion.area
        is DeteccionArea.Ambigua -> deteccion.candidatas.first()
        DeteccionArea.Ninguna -> null
    }

    /** Solo el área que se puede asignar sin preguntar. */
    fun areaDetectada(nombreAsignatura: String): String? = (detectarArea(nombreAsignatura) as? DeteccionArea.Unica)?.area

    /**
     * El tema del catálogo de [area] que menciona un texto (el título de una tarea): "Taller de integrales" → Integrales.
     * Compara raíces de palabra (6 letras) para aceptar singular y plural. Si ninguno o varios empatan, null:
     * mejor no elegir que elegir mal.
     */
    fun temaEnTexto(area: String, texto: String): String? {
        val palabras = palabrasDe(texto)
        if (palabras.isEmpty()) return null
        val puntajes = temasDe(area).associateWith { tema ->
            palabrasDe(tema).filter { it.length >= 4 }.count { clave ->
                val raiz = clave.take(6)
                palabras.any { it.startsWith(raiz) || (it.length >= 5 && raiz.startsWith(it)) }
            }
        }.filterValues { it > 0 }
        val mejor = puntajes.values.maxOrNull() ?: return null
        return puntajes.filterValues { it == mejor }.keys.singleOrNull()
    }

    private fun palabrasDe(texto: String) = normalizar(texto).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

    /** Minúsculas y sin tildes, para comparar nombres escritos de distintas formas. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    // El orden importa para la sugerencia de un nombre ambiguo: "termodinamica quimica" sugiere Quimica antes que Fisica
    private val PALABRAS_POR_AREA: Map<String, List<String>> = linkedMapOf(
        "Quimica" to listOf("quimic", "fisicoquimic", "bioquimic", "estequiometr"),
        "Biologia" to listOf("biolog", "anatom", "genetic", "microbio", "ecolog", "botanic", "zoolog", "ciencias naturales"),
        "Fisica" to listOf("fisica", "mecanica", "electromagnet", "electricidad", "optica", "termodinamica"),
        "Programacion" to listOf("program", "algoritm", "software", "base de datos", "bases de datos", "desarrollo web", "inteligencia artificial", "computacion", "informatica", "sistemas"),
        "Matematicas" to listOf("matemat", "calculo", "precalculo", "algebra", "estadistic", "probabilidad", "trigonometr", "geometr", "aritmet", "ecuacion"),
        "Geografia" to listOf("geograf", "cartograf", "climatolog", "geopolitic"),
        "Historia" to listOf("histori"),
        "Ingles" to listOf("ingles", "english"),
        "Literatura" to listOf("literatur", "lengua castellana", "lenguaje", "espanol", "lectura", "redaccion", "poesia", "teatro"),
        "Arte" to listOf("arte", "artistic", "dibujo", "diseno", "pintura", "escultura")
    )

    private val ETIQUETAS: Map<String, String> = mapOf(
        "Biologia" to "Biología", "Fisica" to "Física", "Geografia" to "Geografía", "Ingles" to "Inglés",
        "Matematicas" to "Matemáticas", "Programacion" to "Programación", "Quimica" to "Química",
        "Apreciacion artistica" to "Apreciación artística", "Diseno grafico" to "Diseño gráfico",
        "Escultura y ceramica" to "Escultura y cerámica", "Tecnicas de dibujo" to "Técnicas de dibujo",
        "Anatomia humana" to "Anatomía humana", "Celula y genetica" to "Célula y genética", "Evolucion" to "Evolución",
        "Microbiologia" to "Microbiología", "Fisica moderna" to "Física moderna", "Mecanica clasica" to "Mecánica clásica",
        "Ondas y optica" to "Ondas y óptica", "Termodinamica" to "Termodinámica", "Cartografia" to "Cartografía",
        "Climatologia" to "Climatología", "Geografia fisica" to "Geografía física", "Geografia humana" to "Geografía humana",
        "Geopolitica" to "Geopolítica", "Geopolitica mundial" to "Geopolítica mundial", "Comprension de lectura" to "Comprensión de lectura",
        "Expresion oral" to "Expresión oral", "Gramatica y sintaxis" to "Gramática y sintaxis", "Vocabulario tematico" to "Vocabulario temático",
        "Analisis literario" to "Análisis literario", "Narrativa latinoamericana" to "Narrativa latinoamericana",
        "Poesia y retorica" to "Poesía y retórica", "Algebra lineal" to "Álgebra lineal", "Calculo diferencial" to "Cálculo diferencial",
        "Probabilidad y estadistica" to "Probabilidad y estadística", "Trigonometria" to "Trigonometría",
        "Programacion orientada a objetos" to "Programación orientada a objetos", "Electroquimica" to "Electroquímica",
        "Enlace quimico" to "Enlace químico", "Estequiometria" to "Estequiometría", "Quimica organica" to "Química orgánica",
        "Termodinamica quimica" to "Termodinámica química", "Ejercicios practicos" to "Ejercicios prácticos",
        "Metodo Feynman" to "Método Feynman", "Sin metodo definido" to "No uso un método definido"
    )
}

/** Resultado de relacionar el nombre de una asignatura con un área del modelo. */
sealed class DeteccionArea {
    data class Unica(val area: String) : DeteccionArea()
    data class Ambigua(val candidatas: List<String>) : DeteccionArea()
    object Ninguna : DeteccionArea()
}
