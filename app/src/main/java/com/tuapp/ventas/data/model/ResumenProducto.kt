package com.tuapp.ventas.data.model

/**
 * Modelo de presentación para el resumen de productos vendidos en un día.
 */
data class ResumenProducto(
    val nombre: String,
    val cantidadVendida: Int,
    val stockDisponible: Int,
    val subtotal: Double
)
