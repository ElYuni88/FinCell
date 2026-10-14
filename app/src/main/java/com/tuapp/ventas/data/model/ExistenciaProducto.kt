package com.tuapp.ventas.data.model

/**
 * Modelo de presentación para mostrar la existencia de un producto.
 *
 * - esHistorico = true → viene de un snapshot del día consultado
 * - esHistorico = false → viene del stock ACTUAL (fallback si no hay snapshot)
 */
data class ExistenciaProducto(
    val nombre: String,
    val stockDisponible: Int,
    val precio: Double,
    val esHistorico: Boolean = false,      // ✅ NUEVO
    val fechaSnapshot: String? = null      // ✅ NUEVO (ej: "2026-10-13")
)