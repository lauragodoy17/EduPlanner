package com.softcg.myapplication

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.softcg.myapplication.domain.preferencias.TemaApp
import com.softcg.myapplication.ui.tema.estilo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Un estilo o atributo mal escrito solo falla al inflar: esto recorre todos los layouts con el tema real. */
@RunWith(AndroidJUnit4::class)
class VistasInflanTest {

    @Test
    fun todosLosLayoutsSeInflanConElTemaDeLaApp() = comprobarCon(R.style.Theme_MyApplication)

    /** Cada tema debe dar valor a todos los colores de la paleta (attrs_tema.xml): si falta uno, falla aquí. */
    @Test
    fun todosLosLayoutsSeInflanConCadaTemaElegible() {
        TemaApp.entries.forEach { comprobarCon(it.estilo, it.nombre) }
    }

    private fun comprobarCon(estilo: Int, nombreTema: String = "base") {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val contexto = ContextThemeWrapper(instrumentation.targetContext, estilo)
        val inflador = LayoutInflater.from(contexto)
        val errores = mutableListOf<String>()

        instrumentation.runOnMainSync {
            R.layout::class.java.fields
                .filterNot { it.name.startsWith("abc_") }
                .filter { it.name in LAYOUTS_PROPIOS || esPropio(contexto, it.getInt(null)) }
                .filterNot { it.name == "activity_home" }
                .forEach { campo ->
                    // Un layout con <merge> solo se puede inflar adjunto a su padre
                    val adjuntar = campo.name == "agenda_calendar_widget"
                    runCatching { inflador.inflate(campo.getInt(null), FrameLayout(contexto), adjuntar) }
                        .onFailure { errores += "${campo.name}: ${it.cause?.message ?: it.message}" }
                }
        }

        assertEquals("Tema $nombreTema:\n" + errores.joinToString("\n"), 0, errores.size)
    }

    /** Los layouts de librerías comparten R; basta con probar los que están en este módulo. */
    private fun esPropio(contexto: ContextThemeWrapper, id: Int): Boolean =
        contexto.resources.getResourceEntryName(id).let { nombre ->
            PREFIJOS_PROPIOS.any { nombre.startsWith(it) }
        }

    private companion object {
        val LAYOUTS_PROPIOS = setOf("custom_non_select_dropdown", "custom_spinner_dropdown_item", "mytoolbar", "header_layout", "floating_bottons", "unified_agenda_card", "unified_item_card", "schedule_asignatura_card", "agenda_calendar_widget")
        val PREFIJOS_PROPIOS = listOf("activity_", "aviso_", "dialog_error", "dialog_subida", "fragment_", "item_", "sheet_", "stat_", "tab_")
    }
}
