package com.tuapp.ventas.data.model

/**
 * Modelo de presentación para mostrar la existencia actual de un producto.
 */
data class ExistenciaProducto(
    val nombre: String,
    val stockDisponible: Int,
    val precio: Double
)
