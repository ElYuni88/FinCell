package com.tuapp.ventas.data.model

/**
 * Modelo de la pestaña "Existencia del día".
 *
 * - stockHistorico = null  → no hay snapshot para esa fecha (P2 = "—")
 * - stockActual    = null  → producto eliminado de la BD (P5 = "—")
 * - esEliminado    → true si estaba en el snapshot y ya no existe
 * - esInactivo     → true si el producto sigue en BD pero inactivo
 */
data class ExistenciaProducto(
    val codigoBarras: String,
    val nombre: String,
    val stockHistorico: Int?,
    val stockActual: Int?,
    val precio: Double,
    val esHistorico: Boolean,
    val fechaSnapshot: String?,
    val horaSnapshot: String?,
    val esEliminado: Boolean = false,
    val esInactivo: Boolean = false
)