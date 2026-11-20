package com.softcg.myapplication.domain.encuesta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasEncuestaTest {

    private val meta = MetaEncuesta("2026-11-19 10:00:00", "id-1", "p-1", "1.0 (1)")

    /** Una encuesta completa y válida; el QR se usó. */
    private fun completa(): MutableMap<String, String> {
        val r = mutableMapOf<String, String>()
        EncuestaCatalogo.preguntas.forEach { p ->
            when (p) {
                is Pregunta.Likert -> r[p.id] = "4"
                is Pregunta.Puntaje -> r[p.id] = "9"
                is Pregunta.Opcion -> r[p.id] = p.opciones.first()
                is Pregunta.Texto -> if (p.obligatoria) r[p.id] = "5"
            }
        }
        return r
    }

    private fun valor(columnas: List<ColumnaEncuesta>, encabezado: String) = columnas.first { it.encabezado == encabezado }.valor

    @Test
    fun `la encuesta tiene las 11 secciones con todas sus preguntas`() {
        assertEquals(11, EncuestaCatalogo.secciones.size)
        assertEquals(listOf(7, 10, 12, 12, 9, 6, 5, 6, 3, 6, 6), EncuestaCatalogo.secciones.map { it.preguntas.size })
        assertEquals("los ids no se repiten", EncuestaCatalogo.preguntas.size, EncuestaCatalogo.preguntas.map { it.id }.toSet().size)
    }

    @Test
    fun `sus calcula impares menos uno y pares cinco menos, por dos y medio`() {
        val r = (1..10).associate { "S2_$it" to if (it % 2 == 1) "5" else "1" }
        assertEquals(100.0, ReglasEncuesta.sus(r)!!, 0.0)
        val neutro = (1..10).associate { "S2_$it" to "3" }
        assertEquals(50.0, ReglasEncuesta.sus(neutro)!!, 0.0)
        // Ejemplo: impares 4 (3 puntos c/u = 15), pares 2 (3 puntos c/u = 15) → 30 × 2,5 = 75
        val ejemplo = (1..10).associate { "S2_$it" to if (it % 2 == 1) "4" else "2" }
        assertEquals(75.0, ReglasEncuesta.sus(ejemplo)!!, 0.0)
        assertNull(ReglasEncuesta.sus(neutro - "S2_10"))
    }

    @Test
    fun `nps se clasifica en promotor pasivo y detractor`() {
        assertEquals("Promotor", ReglasEncuesta.categoriaNps(10))
        assertEquals("Promotor", ReglasEncuesta.categoriaNps(9))
        assertEquals("Pasivo", ReglasEncuesta.categoriaNps(7))
        assertEquals("Detractor", ReglasEncuesta.categoriaNps(6))
        assertEquals("Detractor", ReglasEncuesta.categoriaNps(0))
        assertNull(ReglasEncuesta.categoriaNps(11))
    }

    @Test
    fun `falta lo obligatorio pero no las abiertas`() {
        val perfil = EncuestaCatalogo.secciones[0]
        assertEquals(perfil.preguntas.map { it.id }, ReglasEncuesta.faltantes(perfil, emptyMap()))
        assertTrue(ReglasEncuesta.faltantes(EncuestaCatalogo.secciones[10], emptyMap()).isEmpty())
        assertNull(ReglasEncuesta.primeraIncompleta(completa()))
    }

    @Test
    fun `otra app exige escribir cual`() {
        val r = completa().apply { this["S1_6"] = "Otra app" }
        assertEquals(listOf("S1_6"), ReglasEncuesta.faltantes(EncuestaCatalogo.secciones[0], r))
        r["S1_6_detalle"] = "Notion"
        assertTrue(ReglasEncuesta.faltantes(EncuestaCatalogo.secciones[0], r).isEmpty())
        val columnas = ReglasEncuesta.columnas(r, meta)
        assertEquals("Notion", valor(columnas, "S1.6 Antes de EduPlanner, ¿cómo organizabas tus actividades académicas? (¿Cuál app?)"))
    }

    @Test
    fun `sin usar el qr no se piden ni se guardan sus preguntas`() {
        val r = completa().apply { this["S7_1"] = EncuestaCatalogo.NO; remove("S7_2"); remove("S7_3"); remove("S7_4"); remove("S7_5") }
        assertTrue(ReglasEncuesta.faltantes(EncuestaCatalogo.secciones[6], r).isEmpty())
        r["S7_2"] = "5" // quedó de antes de cambiar a "No"
        val columnas = ReglasEncuesta.columnas(r, meta)
        assertNull("se envía como celda vacía", valor(columnas, ReglasEncuesta.encabezado(EncuestaCatalogo.pregunta("S7_2")!!)))
        assertNull(valor(columnas, "Promedio QR"))
    }

    @Test
    fun `los puntajes de impacto van de 1 a 10`() {
        val impacto = EncuestaCatalogo.secciones[9]
        val r = completa().apply { this["S10_1"] = "0"; this["S10_5"] = "11"; this["S10_6"] = "10" }
        assertEquals(listOf("S10_1", "S10_5"), ReglasEncuesta.faltantes(impacto, r))
    }

    @Test
    fun `una respuesta fuera de rango no cuenta`() {
        val r = completa().apply { this["S3_1"] = "7"; this["S1_1"] = "99 años" }
        assertEquals(listOf("S3_1"), ReglasEncuesta.faltantes(EncuestaCatalogo.secciones[2], r))
        assertEquals(listOf("S1_1"), ReglasEncuesta.faltantes(EncuestaCatalogo.secciones[0], r))
    }

    @Test
    fun `la fila lleva metadatos, numeros como numeros, sus, nps y promedios`() {
        val r = completa().apply { this["S3_1"] = "5"; this["S11_1"] = "  Las misiones  " }
        val columnas = ReglasEncuesta.columnas(r, meta)
        assertEquals(
            listOf(ReglasEncuesta.COL_FECHA, ReglasEncuesta.COL_ID, ReglasEncuesta.COL_PARTICIPANTE, ReglasEncuesta.COL_VERSION),
            columnas.take(4).map { it.encabezado }
        )
        assertEquals("id-1", valor(columnas, ReglasEncuesta.COL_ID))
        assertEquals(5, valor(columnas, "S3.1 Registrar tareas, incluida la fecha y la hora de entrega, fue rápido y sencillo."))
        assertEquals(9, valor(columnas, "S9.3 Del 0 al 10, ¿qué tan probable es que recomiendes EduPlanner a un compañero?"))
        assertEquals("Las misiones", valor(columnas, "S11.1 ¿Qué fue lo que más te gustó de EduPlanner?"))
        // Todo en 4: impares 3 puntos, pares 1 punto → 20 × 2,5
        assertEquals(50.0, valor(columnas, ReglasEncuesta.COL_SUS))
        assertEquals("Promotor", valor(columnas, ReglasEncuesta.COL_NPS_CATEGORIA))
        // Organización: once 4 y un 5 → 49 / 12 = 4,08
        assertEquals(4.08, valor(columnas, "Promedio Organización"))
        assertEquals(4.0, valor(columnas, "Promedio Gamificación"))
        // Impacto: cinco puntajes de 1 a 10 en 9; "¿seguirías usándola?" es de opciones y no cuenta
        assertEquals(9.0, valor(columnas, "Promedio Impacto"))
        assertEquals("No", valor(columnas, "S10.4 Después de esta prueba, ¿seguirías usando EduPlanner?"))
        assertEquals("encabezados únicos", columnas.size, columnas.map { it.encabezado }.toSet().size)
    }
}
