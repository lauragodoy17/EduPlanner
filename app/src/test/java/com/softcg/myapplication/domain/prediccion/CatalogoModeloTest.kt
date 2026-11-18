package com.softcg.myapplication.domain.prediccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoModeloTest {

    @Test
    fun `el catalogo coincide con el de la API`() {
        // GET /metadata: 10 áreas con 5 temas cada una y 12 métodos
        assertEquals(10, CatalogoModelo.AREAS.size)
        assertTrue(CatalogoModelo.TEMAS_POR_AREA.values.all { it.size == 5 })
        assertEquals(50, CatalogoModelo.TEMAS_POR_AREA.values.flatten().toSet().size)
        assertEquals(12, CatalogoModelo.METODOS.size)
        // Sin tildes, como en el entrenamiento
        val todo = CatalogoModelo.AREAS + CatalogoModelo.TEMAS_POR_AREA.values.flatten() + CatalogoModelo.METODOS
        assertTrue(todo.none { Regex("[áéíóúñ]", RegexOption.IGNORE_CASE).containsMatchIn(it) })
    }

    @Test
    fun `sugiere el area por el nombre de la asignatura`() {
        assertEquals("Matematicas", CatalogoModelo.sugerirArea("Cálculo II"))
        assertEquals("Matematicas", CatalogoModelo.sugerirArea("Estadística"))
        assertEquals("Fisica", CatalogoModelo.sugerirArea("FÍSICA mecánica"))
        assertEquals("Quimica", CatalogoModelo.sugerirArea("Fisicoquímica"))
        assertEquals("Quimica", CatalogoModelo.sugerirArea("Química orgánica"))
        assertEquals("Programacion", CatalogoModelo.sugerirArea("Programación orientada a objetos"))
        assertEquals("Ingles", CatalogoModelo.sugerirArea("Inglés IV"))
        assertEquals("Historia", CatalogoModelo.sugerirArea("Historia de Colombia"))
        assertEquals("Arte", CatalogoModelo.sugerirArea("Artes plásticas"))
        assertEquals("Literatura", CatalogoModelo.sugerirArea("Lengua castellana"))
    }

    @Test
    fun `sin coincidencia clara no sugiere nada`() {
        assertNull(CatalogoModelo.sugerirArea("Ética"))
        assertNull(CatalogoModelo.sugerirArea("Departamento de deportes"))
        assertNull(CatalogoModelo.sugerirArea(""))
    }

    @Test
    fun `reconoce el tema del catalogo en el titulo de una tarea`() {
        assertEquals("Integrales", CatalogoModelo.temaEnTexto("Matematicas", "Taller de integrales"))
        assertEquals("Algebra lineal", CatalogoModelo.temaEnTexto("Matematicas", "Quiz de álgebra lineal: matrices"))
        assertEquals("Trigonometria", CatalogoModelo.temaEnTexto("Matematicas", "Identidades trigonométricas"))
        assertEquals("Historia antigua", CatalogoModelo.temaEnTexto("Historia", "Ensayo de historia antigua"))
        // "historia" sola está en tres temas: no se adivina
        assertNull(CatalogoModelo.temaEnTexto("Historia", "Taller de historia"))
        assertNull(CatalogoModelo.temaEnTexto("Matematicas", "Parcial 2"))
    }

    @Test
    fun `los metodos que recomienda el modelo son del catalogo`() {
        assertEquals(8, CatalogoModelo.METODOS_RECOMENDABLES.size)
        assertTrue(CatalogoModelo.METODOS_RECOMENDABLES.all { CatalogoModelo.esMetodo(it) })
    }

    @Test
    fun `valida areas, temas y metodos`() {
        assertTrue(CatalogoModelo.esTema("Matematicas", "Integrales"))
        assertFalse(CatalogoModelo.esTema("Fisica", "Integrales"))
        assertFalse(CatalogoModelo.esTema(null, "Integrales"))
        assertFalse(CatalogoModelo.esArea("Matemáticas"))
        assertTrue(CatalogoModelo.esMetodo("Metodo Feynman"))
        assertEquals("Método Feynman", CatalogoModelo.etiqueta("Metodo Feynman"))
        assertEquals("Álgebra lineal", CatalogoModelo.etiqueta("Algebra lineal"))
    }
}
