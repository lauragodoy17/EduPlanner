package com.softcg.myapplication.data.network

import com.softcg.myapplication.domain.ia.FuentePropuesta
import com.softcg.myapplication.domain.ia.TipoFuente
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerificadorFuentesWebTest {

    private val stewart = FuentePropuesta(TipoFuente.LIBRO, "Cálculo de una variable", "James Stewart", "")

    private val googleBooks = """
        {"items": [
          {"volumeInfo": {"title": "Otro libro", "authors": ["James Stewart"], "infoLink": "http://books.google.com/x"}},
          {"volumeInfo": {"title": "Cálculo de una variable", "subtitle": "Trascendentes tempranas",
            "authors": ["James Stewart"], "publishedDate": "2012-03-01",
            "canonicalVolumeLink": "http://books.google.com/books/about/Calculo.html?id=abc"}}
        ]}
    """.trimIndent()

    @Test
    fun `un libro solo vale si coinciden titulo y autor, y se muestra con los datos del catalogo`() {
        val fuente = VerificadorFuentesWeb.libroCoincidente(googleBooks, stewart)!!

        assertEquals("Cálculo de una variable: Trascendentes tempranas", fuente.titulo)
        assertEquals("James Stewart · 2012", fuente.detalle)
        assertEquals("https://books.google.com/books/about/Calculo.html?id=abc", fuente.url)

        assertNull(VerificadorFuentesWeb.libroCoincidente(googleBooks, stewart.copy(autor = "Tom Apostol")))
        assertNull(VerificadorFuentesWeb.libroCoincidente(googleBooks, stewart.copy(titulo = "Libro inventado")))
        assertNull(VerificadorFuentesWeb.libroCoincidente("{}", stewart))
    }

    @Test
    fun `el apellido es la ultima palabra del primer autor`() {
        assertEquals("stewart", VerificadorFuentesWeb.apellidoDe("James Stewart"))
        assertEquals("garcia", VerificadorFuentesWeb.apellidoDe("Ana García, Luis Pérez"))
        assertNull(VerificadorFuentesWeb.apellidoDe(""))
    }

    @Test
    fun `una pagina vale si abre bien, es HTML y no es de error`() {
        val ok = RespuestaGet(200, "https://es.khanacademy.org/math/integral", "text/html; charset=utf-8",
            "<html><head><title>Integrales | Khan Academy</title></head></html>")
        val fuente = VerificadorFuentesWeb.paginaValida(ok)!!
        assertEquals("Integrales | Khan Academy", fuente.titulo)
        assertEquals("es.khanacademy.org", fuente.detalle)

        assertNull(VerificadorFuentesWeb.paginaValida(ok.copy(codigo = 404)))
        assertNull(VerificadorFuentesWeb.paginaValida(ok.copy(tipoContenido = "application/pdf")))
        assertNull(VerificadorFuentesWeb.paginaValida(ok.copy(urlFinal = "http://es.khanacademy.org")))
        assertNull(VerificadorFuentesWeb.paginaValida(ok.copy(cuerpo = "<title>Página no encontrada</title>")))
        assertNull(VerificadorFuentesWeb.paginaValida(ok.copy(cuerpo = "<html>sin título</html>")))
    }

    @Test
    fun `descarta lo que no se puede comprobar sin romper el resto`() = runTest {
        val http = object : ClienteHttp {
            override suspend fun get(url: String): RespuestaGet = when {
                url.startsWith("https://www.googleapis.com/books") -> RespuestaGet(200, url, "application/json", googleBooks)
                url == "https://es.wikipedia.org/wiki/Integraci%C3%B3n_por_partes" ->
                    RespuestaGet(200, url, "text/html", "<title>Integración por partes - Wikipedia, la enciclopedia libre</title>")
                else -> throw java.io.IOException("sin respuesta")
            }
        }
        val verificador = VerificadorFuentesWeb(http)

        val fuentes = verificador.verificar(
            listOf(
                stewart,
                FuentePropuesta(TipoFuente.WEB, "Integración por partes", "Wikipedia", "https://es.wikipedia.org/wiki/Integraci%C3%B3n_por_partes"),
                FuentePropuesta(TipoFuente.WEB, "Inventada", "Nadie", "https://no.existe/x"),
                FuentePropuesta(TipoFuente.WEB, "Sin https", "Nadie", "http://ejemplo.org")
            )
        )

        assertEquals(
            listOf("Cálculo de una variable: Trascendentes tempranas", "Integración por partes - Wikipedia, la enciclopedia libre"),
            fuentes.map { it.titulo }
        )
    }
}
