package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.RelojFijo
import com.softcg.myapplication.core.ejecutarTransaccionesDirectamente
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.network.SesionUsuario
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class TiendaUseCasesTest {

    @RelaxedMockK private lateinit var repository: GamificacionRepository
    @RelaxedMockK private lateinit var sesion: SesionUsuario

    private val reloj = RelojFijo(LocalDateTime.of(2026, 10, 28, 10, 0))
    private lateinit var comprar: ComprarAccesorioUseCase
    private lateinit var equipar: EquiparAccesorioUseCase

    /** Gorra: 50 EduCoins, categoría CABEZA. */
    private val gorra = AccesoriosCatalogo.buscar("gorra")!!

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        every { sesion.uid() } returns "usuario-1"
        coEvery { repository.posee(any(), any()) } returns false
        coEvery { repository.saldo(any()) } returns 100
        coEvery { repository.xpTotal(any()) } returns 0
        coEvery { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) } returns true
        repository.ejecutarTransaccionesDirectamente()
        comprar = ComprarAccesorioUseCase(repository, sesion, reloj)
        equipar = EquiparAccesorioUseCase(repository, sesion)
    }

    // Compras

    @Test
    fun `sin EduCoins no se puede comprar y se informa precio y saldo`() = runBlocking {
        coEvery { repository.saldo("usuario-1") } returns 0

        assertEquals(ResultadoCompra.SaldoInsuficiente(precio = 50, saldo = 0), comprar("gorra"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.agregarAlInventario(any(), any(), any()) }
    }

    @Test
    fun `con saldo suficiente se cobra el precio del catalogo y se agrega al inventario`() = runBlocking {
        assertEquals(ResultadoCompra.Comprado(gorra, saldoRestante = 50), comprar("gorra"))

        coVerify { repository.insertarMovimiento("usuario-1", "COMPRA:gorra", "COMPRA", -50, "Compraste Gorra", any(), "2026-10-28") }
        coVerify { repository.agregarAlInventario("usuario-1", "gorra", any()) }
    }

    @Test
    fun `con saldo exactamente igual al precio la compra se realiza y el saldo queda en cero`() = runBlocking {
        coEvery { repository.saldo("usuario-1") } returns 50

        assertEquals(ResultadoCompra.Comprado(gorra, saldoRestante = 0), comprar("gorra"))
    }

    @Test
    fun `no se puede comprar un accesorio que ya se tiene`() = runBlocking {
        coEvery { repository.posee("usuario-1", "gorra") } returns true

        assertEquals(ResultadoCompra.YaLoTiene, comprar("gorra"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `reintentar una compra ya cobrada no cobra dos veces`() = runBlocking {
        // El cobro anterior existe: la clave COMPRA:gorra es única y el insert se ignora
        coEvery { repository.insertarMovimiento(any(), "COMPRA:gorra", any(), any(), any(), any(), any()) } returns false

        assertEquals(ResultadoCompra.YaLoTiene, comprar("gorra"))
        coVerify(exactly = 0) { repository.agregarAlInventario(any(), any(), any()) }
    }

    @Test
    fun `un error al guardar la compra no se informa como exito`() = runBlocking {
        coEvery { repository.agregarAlInventario(any(), any(), any()) } throws IllegalStateException()

        assertEquals(ResultadoCompra.Error, comprar("gorra"))
    }

    @Test
    fun `un accesorio que no esta en el catalogo no se puede comprar`() = runBlocking {
        assertEquals(ResultadoCompra.NoExiste, comprar("capa_invisible"))
    }

    @Test
    fun `la compra usa el inventario y el saldo del usuario con sesion`() = runBlocking {
        every { sesion.uid() } returns "usuario-2"

        comprar("gorra")

        coVerify { repository.saldo("usuario-2") }
        coVerify { repository.agregarAlInventario("usuario-2", "gorra", any()) }
        coVerify(exactly = 0) { repository.saldo("usuario-1") }
    }

    @Test
    fun `todos los accesorios del catalogo tienen id unico y precio positivo salvo los exclusivos de logro`() {
        val ids = AccesoriosCatalogo.accesorios.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        AccesoriosCatalogo.accesorios.forEach {
            assertTrue("${it.id} sin precio", it.precio > 0 || it.logroRequerido != null)
        }
    }

    @Test
    fun `los accesorios exclusivos apuntan a un logro que existe`() {
        AccesoriosCatalogo.accesorios.mapNotNull { it.logroRequerido }.forEach {
            assertTrue("$it no existe", LogrosCatalogo.buscar(it) != null)
        }
    }

    @Test
    fun `un accesorio con nivel minimo no se vende antes de ese nivel aunque alcance el saldo`() = runBlocking {
        coEvery { repository.xpTotal("usuario-1") } returns 0
        coEvery { repository.saldo("usuario-1") } returns 1000

        assertEquals(ResultadoCompra.NivelInsuficiente(3), comprar("audifonos"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `al llegar al nivel minimo el accesorio ya se puede comprar`() = runBlocking {
        coEvery { repository.xpTotal("usuario-1") } returns 150

        assertEquals(ResultadoCompra.Comprado(AccesoriosCatalogo.buscar("audifonos")!!, saldoRestante = 10), comprar("audifonos"))
    }

    @Test
    fun `gastar monedas no baja de nivel porque el nivel sale del XP, no del saldo`() = runBlocking {
        coEvery { repository.xpTotal("usuario-1") } returns 150
        coEvery { repository.saldo("usuario-1") } returns 0

        assertEquals(ResultadoCompra.SaldoInsuficiente(90, 0), comprar("audifonos"))
    }

    @Test
    fun `un accesorio exclusivo de logro no se puede comprar`() = runBlocking {
        coEvery { repository.saldo("usuario-1") } returns 1000

        assertEquals(ResultadoCompra.SoloPorLogro, comprar("corona_laurel"))
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `quien ya tenia un accesorio ahora con nivel minimo lo conserva`() = runBlocking {
        coEvery { repository.posee("usuario-1", "birrete") } returns true

        assertEquals(ResultadoEquipar.Equipado, equipar.equipar("birrete"))
    }

    @Test
    fun `cambiar la apariencia guarda los tonos y rechaza los que no existen`() = runBlocking {
        val cambiar = CambiarAparienciaUseCase(repository, sesion)

        assertEquals(true, cambiar(AvatarBase(2, 4)))
        coVerify { repository.guardarApariencia("usuario-1", 2, 4) }

        assertEquals(false, cambiar(AvatarBase(AvatarBase.TONOS, 0)))
        coVerify(exactly = 1) { repository.guardarApariencia(any(), any(), any()) }
    }

    // Equipamiento

    @Test
    fun `un accesorio comprado se equipa en su categoria`() = runBlocking {
        coEvery { repository.posee("usuario-1", "gorra") } returns true

        assertEquals(ResultadoEquipar.Equipado, equipar.equipar("gorra"))
        coVerify { repository.equipar("usuario-1", "CABEZA", "gorra") }
    }

    @Test
    fun `no se puede equipar un accesorio que no se compro`() = runBlocking {
        assertEquals(ResultadoEquipar.NoLoPosee, equipar.equipar("birrete"))
        coVerify(exactly = 0) { repository.equipar(any(), any(), any()) }
    }

    @Test
    fun `equipar otro accesorio de la misma categoria lo reemplaza`() = runBlocking {
        coEvery { repository.posee("usuario-1", any()) } returns true

        equipar.equipar("gorra")
        equipar.equipar("birrete")

        // Una fila por categoría: el segundo reemplaza al primero
        coVerify { repository.equipar("usuario-1", "CABEZA", "gorra") }
        coVerify { repository.equipar("usuario-1", "CABEZA", "birrete") }
    }

    @Test
    fun `retirar un accesorio no lo quita del inventario ni devuelve monedas`() = runBlocking {
        assertEquals(ResultadoEquipar.Retirado, equipar.retirar(CategoriaAccesorio.CABEZA))

        coVerify { repository.retirar("usuario-1", "CABEZA") }
        coVerify(exactly = 0) { repository.insertarMovimiento(any(), any(), any(), any(), any(), any(), any()) }
    }

    // Avatar

    @Test
    fun `la apariencia base del avatar es estable para el mismo usuario`() {
        val a = AvatarBase.desdeUid("abc123")
        repeat(5) { assertEquals(a, AvatarBase.desdeUid("abc123")) }
        listOf("", "x", "usuario-largo-con-muchos-caracteres").forEach {
            val base = AvatarBase.desdeUid(it)
            assert(base.indicePiel in 0 until AvatarBase.TONOS && base.indicePelo in 0 until AvatarBase.TONOS)
        }
    }
}
