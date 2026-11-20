package com.softcg.myapplication.domain.encuesta

/** Una pregunta solo se muestra (y se exige) si otra tiene cierta respuesta, p. ej. el QR solo si se usó. */
data class Condicion(val preguntaId: String, val valor: String)

sealed class Pregunta {
    /** Identificador estable ("S2_3"); también ordena las columnas de la hoja. No cambiar: es la clave de la respuesta. */
    abstract val id: String
    abstract val texto: String
    abstract val obligatoria: Boolean
    abstract val soloSi: Condicion?

    /** De 1 (totalmente en desacuerdo) a 5 (totalmente de acuerdo); se guarda como número. */
    data class Likert(override val id: String, override val texto: String, override val soloSi: Condicion? = null) : Pregunta() {
        override val obligatoria = true
    }

    /**
     * Una opción entre varias. Si se elige [conDetalle] (p. ej. "Otra app"), se pide además un texto corto
     * ([detalleHint]), que se guarda en la columna "<id>_detalle".
     */
    data class Opcion(
        override val id: String,
        override val texto: String,
        val opciones: List<String>,
        val conDetalle: String? = null,
        val detalleHint: String = "",
        override val soloSi: Condicion? = null
    ) : Pregunta() {
        override val obligatoria = true
    }

    /**
     * Puntaje en un rango numérico con el significado de sus extremos: la recomendación de 0 a 10 (NPS) y las
     * preguntas de impacto de 1 a 10. Se guarda como número.
     */
    data class Puntaje(
        override val id: String,
        override val texto: String,
        val desde: Int,
        val hasta: Int,
        val minimo: String,
        val maximo: String
    ) : Pregunta() {
        override val obligatoria = true
        override val soloSi: Condicion? = null
        val rango: IntRange get() = desde..hasta
    }

    /** Respuesta escrita: corta (una línea, p. ej. el semestre) o abierta (varias líneas, opcional). */
    data class Texto(
        override val id: String,
        override val texto: String,
        override val obligatoria: Boolean,
        val maximo: Int,
        val varias: Boolean,
        val hint: String = "",
        override val soloSi: Condicion? = null
    ) : Pregunta()
}

/**
 * Una página de la encuesta. [promedio] es el nombre del bloque en la hoja ("Organización") si sus preguntas
 * de 1 a 5 se promedian para comparar módulos (en Impacto, que no tiene, las de 1 a 10); null para el perfil, el
 * SUS (tiene su propio puntaje) y las abiertas.
 */
data class SeccionEncuesta(
    val numero: Int,
    val titulo: String,
    val descripcion: String,
    val preguntas: List<Pregunta>,
    val promedio: String? = null
)

/** La encuesta de evaluación de EduPlanner, tal como se aplica a los participantes. */
object EncuestaCatalogo {

    const val ESCALA_MIN = "Totalmente en desacuerdo"
    const val ESCALA_MAX = "Totalmente de acuerdo"
    const val SI = "Sí"
    const val NO = "No"

    private const val INSTRUCCION_LIKERT = "Marca de 1 ($ESCALA_MIN) a 5 ($ESCALA_MAX)."
    private const val MAX_ABIERTA = 500

    private fun likert(seccion: Int, textos: List<String>, desde: Int = 1, soloSi: Condicion? = null) =
        textos.mapIndexed { i, texto -> Pregunta.Likert("S${seccion}_${desde + i}", texto, soloSi) }

    private val usoQr = Condicion("S7_1", SI)

    val secciones: List<SeccionEncuesta> = listOf(
        SeccionEncuesta(
            1, "Perfil del participante", "Cuéntanos un poco sobre ti y cómo usaste EduPlanner.",
            listOf(
                Pregunta.Opcion("S1_1", "Edad", listOf("<18", "18–20", "21–23", "24–26", ">26")),
                Pregunta.Opcion("S1_2", "Nivel educativo", listOf("Secundaria", "Técnico/Tecnológico", "Pregrado", "Posgrado")),
                Pregunta.Texto("S1_3", "Semestre o grado que cursas", obligatoria = true, maximo = 40, varias = false, hint = "Ej.: 5.º semestre, 11.º grado"),
                Pregunta.Opcion("S1_4", "¿Cuánto tiempo usaste EduPlanner?", listOf("<1 semana", "1–2 semanas", "3–4 semanas", ">1 mes")),
                Pregunta.Opcion("S1_5", "¿Con qué frecuencia la abriste?", listOf("Varias veces al día", "Una vez al día", "Algunas veces por semana", "Rara vez")),
                Pregunta.Opcion(
                    "S1_6", "Antes de EduPlanner, ¿cómo organizabas tus actividades académicas?",
                    listOf("Agenda física", "Calendario del celular", "Otra app", "No las organizaba"),
                    conDetalle = "Otra app", detalleHint = "¿Cuál app?"
                ),
                Pregunta.Opcion("S1_7", "¿Cómo iniciaste sesión?", listOf("Correo y contraseña", "Cuenta de Google"))
            )
        ),
        SeccionEncuesta(
            2, "Usabilidad general", INSTRUCCION_LIKERT,
            likert(
                2, listOf(
                    "Creo que me gustaría usar EduPlanner con frecuencia.",
                    "Encontré la aplicación innecesariamente compleja.",
                    "Pensé que la aplicación era fácil de usar.",
                    "Creo que necesitaría ayuda de una persona con conocimientos técnicos para usarla.",
                    "Las funciones de la aplicación están bien integradas.",
                    "Pensé que había demasiada inconsistencia en la aplicación.",
                    "Imagino que la mayoría de las personas aprendería a usarla muy rápido.",
                    "Encontré la aplicación muy difícil de usar.",
                    "Me sentí con confianza al usar la aplicación.",
                    "Necesité aprender muchas cosas antes de poder usarla."
                )
            )
        ),
        SeccionEncuesta(
            3, "Organización académica", "Funciones principales. $INSTRUCCION_LIKERT",
            likert(
                3, listOf(
                    "Registrar tareas, incluida la fecha y la hora de entrega, fue rápido y sencillo.",
                    "Crear y gestionar mis asignaturas fue fácil.",
                    "Fue útil que cada tarea tenga que pertenecer a una asignatura.",
                    "La Agenda/calendario me ayudó a ver mis actividades y eventos.",
                    "El horario de clases me resultó claro y útil.",
                    "Registrar y consultar mis calificaciones fue sencillo.",
                    "Entregar y calificar una tarea desde su tarjeta fue intuitivo.",
                    "Las pantallas de detalle (\"Ver más\") muestran la información que necesito.",
                    "Editar, borrar y compartir elementos desde sus pantallas fue fácil.",
                    "El panel de inicio me da una buena visión de lo que tengo pendiente.",
                    "Desde que uso EduPlanner siento que organizo mejor mis actividades académicas.",
                    "Desde que uso EduPlanner entrego mis tareas a tiempo con más frecuencia."
                )
            ),
            promedio = "Organización"
        ),
        SeccionEncuesta(
            4, "Gamificación", INSTRUCCION_LIKERT,
            likert(
                4, listOf(
                    "Ganar EduCoins me motivó a registrar y completar mis actividades.",
                    "El sistema de niveles y XP me animó a seguir usando la app.",
                    "La racha me motivó a usar la app de forma constante.",
                    "Los logros me resultaron atractivos y comprensibles.",
                    "Las misiones me dieron objetivos claros y alcanzables.",
                    "La tienda y la personalización del avatar me parecieron entretenidas.",
                    "Las sesiones de enfoque me ayudaron a concentrarme al estudiar.",
                    "Confirmar mi asistencia a clase fue fácil y no me resultó molesto.",
                    "Las celebraciones (subida de nivel, logros) y los mensajes de motivación fueron agradables.",
                    "Las recompensas me parecieron justas y equilibradas.",
                    "La gamificación aumentó mi motivación para estudiar, no solo para usar la app."
                )
            ) + Pregunta.Opcion(
                "S4_12", "¿Qué elemento te motivó más?",
                listOf("EduCoins", "Niveles", "Racha", "Logros", "Misiones", "Tienda/Avatar", "Enfoque", "Ninguno")
            ),
            promedio = "Gamificación"
        ),
        SeccionEncuesta(
            5, "Predicciones y recomendaciones con IA", INSTRUCCION_LIKERT,
            likert(
                5, listOf(
                    "Entendí para qué sirve la sección \"Predicción y recomendaciones\" de una tarea.",
                    "Las preguntas para completar mis datos (área de la asignatura, horas de estudio, método, interés y dificultad) fueron claras.",
                    "Responder esas preguntas no me pareció invasivo ni tedioso.",
                    "El método de estudio recomendado se ajustó a mi forma de estudiar.",
                    "Las explicaciones y consejos de la recomendación fueron claros y útiles.",
                    "Las fuentes web sugeridas fueron pertinentes y confiables.",
                    "Confié en las recomendaciones que me dio la aplicación.",
                    "La recomendación llegó en un tiempo de espera aceptable."
                )
            ) + Pregunta.Opcion(
                "S5_9", "Si aplicaste alguna recomendación, ¿cómo te fue?",
                listOf("Mejor que antes", "Igual", "Peor", "No la apliqué")
            ),
            promedio = "IA"
        ),
        SeccionEncuesta(
            6, "Notificaciones", INSTRUCCION_LIKERT,
            likert(
                6, listOf(
                    "El aviso 10 minutos antes de clase fue útil.",
                    "El resumen diario de las 9:00 me ayudó a planear mi día.",
                    "Los recordatorios antes del vencimiento de una tarea (1 hora y 5 minutos) llegaron a tiempo.",
                    "Poder responder la asistencia (\"Sí, asistí\" / \"No asistí\") desde la notificación fue práctico.",
                    "La cantidad de notificaciones fue adecuada, no excesiva.",
                    "Al tocar una notificación llegué a la pantalla correcta."
                )
            ),
            promedio = "Notificaciones"
        ),
        SeccionEncuesta(
            7, "Compartir por QR", "Si no usaste esta función, responde \"No\" y continúa.",
            listOf(Pregunta.Opcion("S7_1", "¿Usaste la función de compartir por QR?", listOf(SI, NO))) + likert(
                7, listOf(
                    "Generar el QR de una tarea, evento, asignatura o calificación fue fácil.",
                    "Escanear un QR e importar el elemento fue rápido y sin errores.",
                    "Fue claro qué pasaba con la asignatura al importar (crear una nueva o reasignar a una existente).",
                    "Compartir por QR me resultó útil para trabajar con mis compañeros."
                ),
                desde = 2, soloSi = usoQr
            ),
            promedio = "QR"
        ),
        SeccionEncuesta(
            8, "Diseño y apariencia", INSTRUCCION_LIKERT,
            likert(
                8, listOf(
                    "El diseño visual de la aplicación es atractivo.",
                    "La navegación (menú lateral, barra inferior, botones flotantes) es clara.",
                    "Los textos y los íconos son fáciles de entender.",
                    "Poder elegir tema (Claro, Oscuro, Azul, Clásico) mejoró mi experiencia."
                )
            ) + Pregunta.Opcion("S8_5", "¿Qué tema usaste más?", listOf("Claro", "Oscuro", "Azul", "Clásico")) +
                Pregunta.Likert("S8_6", "La aplicación funcionó de forma fluida, sin fallas ni cierres inesperados."),
            promedio = "Diseño"
        ),
        SeccionEncuesta(
            9, "Satisfacción general", INSTRUCCION_LIKERT,
            likert(
                9, listOf(
                    "En general, estoy satisfecho/a con EduPlanner.",
                    "EduPlanner cumple con lo que esperaba de una aplicación de planificación académica."
                )
            ) + Pregunta.Puntaje(
                "S9_3", "Del 0 al 10, ¿qué tan probable es que recomiendes EduPlanner a un compañero?",
                0, 10, "Nada probable", "Muy probable"
            ),
            promedio = "Satisfacción"
        ),
        SeccionEncuesta(
            10, "Impacto en tu vida estudiantil", "Marca de 1 a 10 según lo que percibiste durante la prueba.",
            listOf(
                Pregunta.Puntaje("S10_1", "Del 1 al 10, ¿qué tanto te ayudaron las recomendaciones con IA?", 1, 10, "Nada", "Muchísimo"),
                Pregunta.Puntaje("S10_2", "Del 1 al 10, ¿cuánta mejoría notaste en tu vida estudiantil?", 1, 10, "Ninguna", "Muchísima"),
                Pregunta.Puntaje("S10_3", "Del 1 al 10, ¿qué tan juiciosamente utilizaste EduPlanner?", 1, 10, "Muy poco", "Totalmente"),
                Pregunta.Opcion(
                    "S10_4", "Después de esta prueba, ¿seguirías usando EduPlanner?",
                    listOf("No", "Casi nunca", "A veces", "Muy seguido", "Siempre")
                ),
                Pregunta.Puntaje("S10_5", "Del 1 al 10, ¿cuánto percibiste que mejoraron tus notas?", 1, 10, "Nada", "Muchísimo"),
                Pregunta.Puntaje("S10_6", "Del 1 al 10, ¿cuánto percibiste que disminuyó tu procrastinación?", 1, 10, "Nada", "Muchísimo")
            ),
            promedio = "Impacto"
        ),
        SeccionEncuesta(
            11, "Preguntas abiertas", "Son opcionales, pero nos ayudan muchísimo a mejorar.",
            listOf(
                "¿Qué fue lo que más te gustó de EduPlanner?",
                "¿Qué fue lo que menos te gustó o te resultó más difícil?",
                "¿Tuviste algún error o problema técnico? Descríbelo.",
                "¿Qué función agregarías o cambiarías?",
                "¿Cómo influyó EduPlanner en tu rendimiento o en tus hábitos de estudio?",
                "Comentarios adicionales."
            ).mapIndexed { i, texto ->
                Pregunta.Texto("S11_${i + 1}", texto, obligatoria = false, maximo = MAX_ABIERTA, varias = true)
            }
        )
    )

    val preguntas: List<Pregunta> = secciones.flatMap { it.preguntas }

    fun pregunta(id: String): Pregunta? = preguntas.firstOrNull { it.id == id }
}
