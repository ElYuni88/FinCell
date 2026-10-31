package com.tuapp.ventas.data.model

/**
 * Modelo de presentación para el resumen de productos vendidos en un período.
 *
 * - cantidadVendida / subtotal      → ventas del período actual
 * - cantidadVendidaMuerto / subtotalMuerto → ventas del período muerto inmediatamente anterior
 */
data class ResumenProducto(
    val nombre: String,
    val cantidadVendida: Int,
    val stockDisponible: Int,
    val subtotal: Double,
    val cantidadVendidaMuerto: Int = 0,
    val subtotalMuerto: Double = 0.0
) {
    val subtotalTotal: Double get() = subtotal + subtotalMuerto
    val cantidadTotal: Int get() = cantidadVendida + cantidadVendidaMuerto
}