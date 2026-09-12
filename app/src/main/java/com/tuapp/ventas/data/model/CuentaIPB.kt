package com.tuapp.ventas.data.model

import com.google.gson.annotations.SerializedName

/**
 * Información detallada de una cuenta cerrada para el reporte IPB.
 * Permite al admin analizar comportamiento de clientes.
 */
data class CuentaIPB(
    @SerializedName("cuenta_id") val cuentaId: Long,
    @SerializedName("cliente_id") val clienteId: Long?,
    @SerializedName("nombre_cliente") val nombreCliente: String,
    val telefono: String? = null,
    val mesa: String? = null,
    @SerializedName("es_cliente_temporal") val esClienteTemporal: Boolean,
    @SerializedName("fecha_apertura") val fechaApertura: Long,
    @SerializedName("fecha_cierre") val fechaCierre: Long,
    val total: Double,
    @SerializedName("metodo_pago") val metodoPago: String,
    @SerializedName("cantidad_productos") val cantidadProductos: Int,
    val productos: List<ProductoCuentaIPB> = emptyList()
)

/**
 * Producto dentro de una cuenta para el reporte IPB.
 */
data class ProductoCuentaIPB(
    val nombre: String,
    @SerializedName("codigo_barras") val codigoBarras: String,
    val cantidad: Int,
    @SerializedName("precio_unitario") val precioUnitario: Double,
    val subtotal: Double
)

/**
 * Resumen de clientes del día.
 */
data class ResumenClientesIPB(
    @SerializedName("total_clientes") val totalClientes: Int,
    @SerializedName("clientes_recurrentes") val clientesRecurrentes: List<ClienteRecurrenteIPB>,
    @SerializedName("mejor_cliente") val mejorCliente: ClienteRecurrenteIPB? = null
)

/**
 * Cliente con múltiples cuentas en el día.
 */
data class ClienteRecurrenteIPB(
    @SerializedName("cliente_id") val clienteId: Long?,
    @SerializedName("nombre_cliente") val nombreCliente: String,
    @SerializedName("cantidad_cuentas") val cantidadCuentas: Int,
    @SerializedName("total_consumido") val totalConsumido: Double,
    @SerializedName("ticket_promedio") val ticketPromedio: Double,
    @SerializedName("metodo_pago_frecuente") val metodoPagoFrecuente: String
)