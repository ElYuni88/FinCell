package com.tuapp.ventas.data.model

/** Producto acumulado dentro de una venta múltiple antes de registrarla. */
data class VentaItem(
    val producto: Producto,
    val cantidad: Int
) {
    val subtotal: Double get() = producto.precio * cantidad
    val id: Long get() = producto.id
}
