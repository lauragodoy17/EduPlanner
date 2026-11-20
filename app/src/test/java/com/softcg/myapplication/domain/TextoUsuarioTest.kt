package com.softcg.myapplication.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoUsuarioTest {

    private val anchoCero = '​'
    private val nulo = '\u0000'
    private val campana = '\u0007'

    @Test
    fun `una linea queda sin saltos, invisibles ni espacios de mas`() {
        assertEquals("Cálculo II", TextoUsuario.linea("  Cálculo\n  II \t", 60))
        // Un invisible pegado dentro de una palabra se borra sin partirla; un carácter de control separa
        assertEquals("Física", TextoUsuario.linea("Fí${anchoCero}sica$nulo", 60))
        assertEquals("a b", TextoUsuario.linea("a${campana}b", 60))
        assertEquals("", TextoUsuario.linea(" \n\t ", 60))
    }

    @Test
    fun `se recorta al maximo aunque se haya pegado mas`() {
        assertEquals(80, TextoUsuario.linea("x".repeat(500), TextoUsuario.MAX_TITULO).length)
        assertEquals(200, TextoUsuario.parrafo("y".repeat(500), TextoUsuario.MAX_DESCRIPCION).length)
        // Sin espacio colgando al cortar
        assertEquals("abc", TextoUsuario.linea("abc def", 4))
    }

    @Test
    fun `un parrafo conserva sus saltos pero no lineas vacias de sobra`() {
        assertEquals("Uno\n\nDos\ntres", TextoUsuario.parrafo("  Uno  \r\n\n\n\n\nDos\ntres  $campana", 200))
    }
}
