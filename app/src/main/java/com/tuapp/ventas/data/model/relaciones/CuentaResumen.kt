package com.tuapp.ventas.data.model.relaciones

/**
 * Proyección ligera para pintar cards de cuentas en la pantalla principal.
 * Incluye cuentas abiertas y cerradas con datos suficientes para UI/UX.
 */
data class CuentaResumen(
    val id: Long,
    val clienteId: Long?,
    val nombreCliente: String,
    val mesa: String?,
    val fechaApertura: Long,
    val fechaCierre: Long?,
    val estado: String,
    val total: Double,
    val cantidadProductos: Int,
    val esClienteTemporal: Boolean
)
