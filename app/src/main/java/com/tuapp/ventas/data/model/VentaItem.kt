package com.tuapp.ventas.data.model

import java.io.Serializable

/** Producto acumulado dentro de una venta múltiple antes de registrarla. */
data class VentaItem(
    val producto: Producto,
    val cantidad: Int
) : Serializable {
    val subtotal: Double get() = producto.precio * cantidad
    val id: Long get() = producto.id
}
