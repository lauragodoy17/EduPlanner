package com.softcg.myapplication.domain.gamificacion

import com.softcg.myapplication.core.Reloj
import com.softcg.myapplication.data.Repositories.GamificacionRepository
import com.softcg.myapplication.data.network.SesionUsuario
import javax.inject.Inject

sealed class ResultadoCompra {
    data class Comprado(val accesorio: Accesorio, val saldoRestante: Int) : ResultadoCompra()
    data class SaldoInsuficiente(val precio: Int, val saldo: Int) : ResultadoCompra()
    data class NivelInsuficiente(val nivelMinimo: Int) : ResultadoCompra()
    object SoloPorLogro : ResultadoCompra()
    object YaLoTiene : ResultadoCompra()
    object NoExiste : ResultadoCompra()
    object SinSesion : ResultadoCompra()
    object Error : ResultadoCompra()
}

class ComprarAccesorioUseCase @Inject constructor(
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario,
    private val reloj: Reloj
) {
    /**
     * El precio sale del catálogo, nunca de la pantalla. Cobro e inventario se guardan en la misma
     * transacción, y la clave "COMPRA:<id>" impide cobrar dos veces el mismo accesorio.
     */
    suspend operator fun invoke(itemId: String): ResultadoCompra {
        val uid = sesion.uid() ?: return ResultadoCompra.SinSesion
        val accesorio = AccesoriosCatalogo.buscar(itemId) ?: return ResultadoCompra.NoExiste
        return try {
            repository.enTransaccion {
                if (repository.posee(uid, itemId)) return@enTransaccion ResultadoCompra.YaLoTiene
                if (accesorio.logroRequerido != null) return@enTransaccion ResultadoCompra.SoloPorLogro
                if (NivelesConfig.progreso(repository.xpTotal(uid)).nivel.numero < accesorio.nivelMinimo) {
                    return@enTransaccion ResultadoCompra.NivelInsuficiente(accesorio.nivelMinimo)
                }

                val saldo = repository.saldo(uid)
                if (saldo < accesorio.precio) {
                    return@enTransaccion ResultadoCompra.SaldoInsuficiente(accesorio.precio, saldo)
                }

                val ahora = reloj.ahora()
                val millis = ahora.toInstant().toEpochMilli()
                val cobrado = repository.insertarMovimiento(
                    uid, "$TIPO_COMPRA:$itemId", TIPO_COMPRA, -accesorio.precio,
                    "Compraste ${accesorio.nombre}", millis, ahora.toLocalDate().toString()
                )
                if (!cobrado) return@enTransaccion ResultadoCompra.YaLoTiene

                repository.agregarAlInventario(uid, itemId, millis)
                ResultadoCompra.Comprado(accesorio, saldo - accesorio.precio)
            }
        } catch (e: Exception) {
            ResultadoCompra.Error
        }
    }
}

sealed class ResultadoEquipar {
    object Equipado : ResultadoEquipar()
    object Retirado : ResultadoEquipar()
    object NoLoPosee : ResultadoEquipar()
    object NoExiste : ResultadoEquipar()
    object SinSesion : ResultadoEquipar()
    object Error : ResultadoEquipar()
}

class EquiparAccesorioUseCase @Inject constructor(
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario
) {
    /** Solo se equipa lo que se compró; equipar reemplaza al accesorio de la misma categoría. */
    suspend fun equipar(itemId: String): ResultadoEquipar {
        val uid = sesion.uid() ?: return ResultadoEquipar.SinSesion
        val accesorio = AccesoriosCatalogo.buscar(itemId) ?: return ResultadoEquipar.NoExiste
        return try {
            if (!repository.posee(uid, itemId)) return ResultadoEquipar.NoLoPosee
            repository.equipar(uid, accesorio.categoria.name, itemId)
            ResultadoEquipar.Equipado
        } catch (e: Exception) {
            ResultadoEquipar.Error
        }
    }

    /** Retirar no quita el accesorio del inventario ni devuelve monedas. */
    suspend fun retirar(categoria: CategoriaAccesorio): ResultadoEquipar {
        val uid = sesion.uid() ?: return ResultadoEquipar.SinSesion
        return try {
            repository.retirar(uid, categoria.name)
            ResultadoEquipar.Retirado
        } catch (e: Exception) {
            ResultadoEquipar.Error
        }
    }
}

class CambiarAparienciaUseCase @Inject constructor(
    private val repository: GamificacionRepository,
    private val sesion: SesionUsuario
) {
    /** false si los tonos no existen o no se pudo guardar. */
    suspend operator fun invoke(base: AvatarBase): Boolean {
        val uid = sesion.uid() ?: return false
        if (base.indicePiel !in 0 until AvatarBase.TONOS || base.indicePelo !in 0 until AvatarBase.TONOS) return false
        return try {
            repository.guardarApariencia(uid, base.indicePiel, base.indicePelo)
            true
        } catch (e: Exception) {
            false
        }
    }
}
