package com.softcg.myapplication.ui.tema

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.softcg.myapplication.data.preferencias.PreferenciasTema
import com.softcg.myapplication.domain.preferencias.TemaApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TemaViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val preferencias: PreferenciasTema = mockk(relaxed = true)

    @Test
    fun `elegir otro tema lo guarda y pide aplicarlo`() {
        every { preferencias.tema() } returns TemaApp.CLARO
        val vm = TemaViewModel(preferencias)

        vm.elegir(TemaApp.OSCURO)

        verify { preferencias.guardar(TemaApp.OSCURO) }
        assertEquals(TemaApp.OSCURO, vm.aplicar.value?.getContentIfNotHandled())
    }

    @Test
    fun `elegir el tema que ya esta no recarga la pantalla`() {
        every { preferencias.tema() } returns TemaApp.AZUL
        val vm = TemaViewModel(preferencias)

        vm.elegir(TemaApp.AZUL)

        verify(exactly = 0) { preferencias.guardar(any()) }
        assertNull(vm.aplicar.value)
    }

    @Test
    fun `se ofrecen los cuatro temas y una clave desconocida vuelve al claro`() {
        assertEquals(listOf("claro", "oscuro", "azul", "clasico"), TemaApp.entries.map { it.clave })
        TemaApp.entries.forEach { assertEquals(it, TemaApp.deClave(it.clave)) }
        assertEquals(TemaApp.CLARO, TemaApp.deClave("morado-neon"))
        assertEquals(TemaApp.CLARO, TemaApp.deClave(null))
    }
}
