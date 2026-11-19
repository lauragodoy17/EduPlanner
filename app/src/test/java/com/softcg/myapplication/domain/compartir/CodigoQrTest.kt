package com.softcg.myapplication.domain.compartir

import com.softcg.myapplication.domain.compartir.ContenidoCompartido.AsignaturaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.EventoCompartido
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.NotaCompartida
import com.softcg.myapplication.domain.compartir.ContenidoCompartido.TareaCompartida
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class CodigoQrTest {

    private val tarea = TareaCompartida("Taller de integrales", "Ejercicios 1 al 10 · ñandú", "Cálculo", "2026-11-20", 1)
    private val evento = EventoCompartido("Feria de ciencias", "", "2026-12-01", "08:00", "12:30", 2)
    private val asignatura = AsignaturaCompartida(
        "Cálculo II", "Ana", 2, "08:00", listOf(true, false, true, false, false, false, false), "Numérico (0-10)", "Matematicas", null, null
    )
    private val nota = NotaCompartida("Parcial", 4.5f, "Cálculo", "Primer corte")

    /** Simula un QR hecho a mano (o por otra app) con este JSON. */
    private fun qrCon(json: JSONObject, version: Int = CodigoQr.VERSION) =
        "${CodigoQr.PREFIJO}$version:" + Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().toByteArray())

    @Test
    fun `cada tipo se lee igual que se escribio, con tildes incluidas`() {
        listOf(tarea, evento, asignatura, nota).forEach { contenido ->
            val texto = CodigoQr.codificar(contenido)
            assertTrue(texto.startsWith("EDUPLANNER:1:"))
            assertEquals(LecturaQr.Valida(contenido), CodigoQr.leer(texto))
        }
    }

    @Test
    fun `los QR que no son de EduPlanner se reconocen al instante`() {
        assertEquals(LecturaQr.NoEsEduPlanner, CodigoQr.leer("https://www.ejemplo.com"))
        assertEquals(LecturaQr.NoEsEduPlanner, CodigoQr.leer("eduplanner:1:abc"))
    }

    @Test
    fun `un QR de una version mas nueva no se malinterpreta`() {
        assertEquals(LecturaQr.VersionNueva, CodigoQr.leer(qrCon(JSONObject().put("k", "t"), version = 2)))
    }

    @Test
    fun `un QR danado o incompleto no crea nada`() {
        assertEquals(LecturaQr.Danado, CodigoQr.leer("EDUPLANNER:1:%%%no-es-base64"))
        assertEquals(LecturaQr.Danado, CodigoQr.leer("EDUPLANNER:x:abc"))
        assertEquals(LecturaQr.Danado, CodigoQr.leer("EDUPLANNER:1"))
        // Tipo desconocido, tarea sin título o con fecha inválida, nota sin asignatura
        assertEquals(LecturaQr.Danado, CodigoQr.leer(qrCon(JSONObject().put("k", "z"))))
        assertEquals(LecturaQr.Danado, CodigoQr.leer(qrCon(JSONObject().put("k", "t").put("ti", " ").put("fe", "2026-11-20"))))
        assertEquals(LecturaQr.Danado, CodigoQr.leer(qrCon(JSONObject().put("k", "t").put("ti", "Taller").put("fe", "mañana"))))
        assertEquals(LecturaQr.Danado, CodigoQr.leer(qrCon(JSONObject().put("k", "n").put("va", 4.0))))
    }

    @Test
    fun `lo que llega de fuera se acota antes de guardarlo`() {
        val json = JSONObject().put("k", "a").put("no", "x".repeat(500)).put("du", 99).put("ho", "ocho")
            .put("ar", "Astrologia").put("mi", 5.0).put("ma", 1.0)
        val leida = (CodigoQr.leer(qrCon(json)) as LecturaQr.Valida).contenido as AsignaturaCompartida

        assertEquals(120, leida.nombre.length)
        assertEquals(12, leida.duracion)
        assertEquals("", leida.hora)
        assertEquals("Un área que no es del catálogo se descarta", null, leida.area)
        assertEquals("Una escala imposible se descarta", null, leida.escalaMin)
        assertEquals(List(7) { false }, leida.dias)

        val conPrioridad = JSONObject().put("k", "e").put("ti", "Charla").put("fe", "2026-11-20").put("pr", 9)
        assertEquals(3, ((CodigoQr.leer(qrCon(conPrioridad)) as LecturaQr.Valida).contenido as EventoCompartido).prioridad)
    }

    @Test
    fun `una descripcion larga cabe en el QR`() {
        // El formulario de tareas permite 200 caracteres: el código debe seguir siendo legible (< 2 KB)
        val larga = tarea.copy(descripcion = "á".repeat(200))
        assertTrue(CodigoQr.codificar(larga).length < 2000)
    }
}
