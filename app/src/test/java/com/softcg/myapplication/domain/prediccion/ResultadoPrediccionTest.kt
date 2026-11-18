package com.softcg.myapplication.domain.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResultadoPrediccionTest {

    // Forma exacta documentada en api.md de modelos_exportados
    private val respuesta = """
        {"prediccion": {"attendance_avg": 6.5, "intensity_hours": 4.0, "grade_avg": 7.0,
          "difficulty_perception": 5.0, "interest_level": 8.0,
          "subject_name": "Matematicas", "subject_topic": "Algebra lineal", "study_method": "Pomodoro",
          "practice_label": "Reforzar", "practice_confianza": 0.61,
          "prob_Debe practicar": 0.12, "prob_Dominada": 0.27, "prob_Reforzar": 0.61,
          "metodo_top1": "Repaso espaciado", "metodo_top1_prob": 0.34,
          "metodo_top2": "Pomodoro", "metodo_top2_prob": 0.21,
          "cluster": 2, "pca_1": -0.53, "pca_2": 1.08},
         "avisos": ["'intensity_hours': 1 fila(s) fuera del rango"]}
    """.trimIndent()

    @Test
    fun `lee estado de practica, metodos en orden de confianza y avisos`() {
        val r = ResultadoPrediccion.desdeJson(respuesta)!!

        assertEquals("Reforzar", r.estadoPractica)
        assertEquals(0.61, r.confianzaPractica!!, 0.0001)
        assertEquals(listOf("Repaso espaciado", "Pomodoro"), r.metodos.map { it.metodo })
        assertEquals("Repaso espaciado", r.principal.metodo)
        assertEquals("34 %", r.principal.porcentaje)
        assertEquals(1, r.avisos.size)
    }

    @Test
    fun `el principal es siempre el de mayor confianza aunque vengan desordenados`() {
        val desordenada = respuesta.replace("\"metodo_top2_prob\": 0.21", "\"metodo_top2_prob\": 0.5")
        assertEquals("Pomodoro", ResultadoPrediccion.desdeJson(desordenada)!!.principal.metodo)
    }

    @Test
    fun `sin metodos validos, o si no es JSON, no hay prediccion`() {
        assertNull(ResultadoPrediccion.desdeJson("""{"prediccion": {"practice_label": "Reforzar"}, "avisos": []}"""))
        assertNull(ResultadoPrediccion.desdeJson("""{"prediccion": {"metodo_top1": "Leer mucho", "metodo_top1_prob": 0.9}}"""))
        assertNull(ResultadoPrediccion.desdeJson("<html>502 Bad Gateway</html>"))
        assertNull(ResultadoPrediccion.desdeJson("""{"detail": "Token de Firebase inválido o ausente."}"""))
    }
}
