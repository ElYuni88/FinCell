package com.tuapp.ventas.data.model

import com.google.gson.annotations.SerializedName

data class ArchivoVentas(
    val version: String = "1.0",
    val fecha: String,
    @SerializedName("timestamp_generacion") val timestampGeneracion: Long,
    @SerializedName("hash_anterior") val hashAnterior: String?,
    val resumen: ResumenVentas,
    @SerializedName("detalle_ventas_simples") val detalleVentasSimples: List<DetalleVentaSimpleExport>,
    @SerializedName("detalle_cuentas") val detalleCuentas: List<DetalleCuentaExport>,
    @SerializedName("hash_total") val hashTotal: String
)

data class ResumenVentas(
    @SerializedName("total_ventas_simples") val totalVentasSimples: Double,
    @SerializedName("total_ventas_cuentas") val totalVentasCuentas: Double,
    @SerializedName("total_general") val totalGeneral: Double,
    @SerializedName("cantidad_ventas_simples") val cantidadVentasSimples: Int,
    @SerializedName("cantidad_cuentas_cerradas") val cantidadCuentasCerradas: Int,
    @SerializedName("producto_mas_vendido") val productoMasVendido: ProductoMasVendido?
)

data class ProductoMasVendido(val nombre: String, val cantidad: Int)

data class DetalleVentaSimpleExport(
    val id: Long,
    @SerializedName("codigo_barras") val codigoBarras: String,
    @SerializedName("nombre_producto") val nombreProducto: String,
    val precio: Double,
    @SerializedName("fecha_venta") val fechaVenta: Long,
    @SerializedName("hash_linea") val hashLinea: String
)

data class DetalleCuentaExport(
    @SerializedName("cuenta_id") val cuentaId: Long,
    @SerializedName("cliente_nombre") val clienteNombre: String,
    val total: Double,
    @SerializedName("fecha_cierre") val fechaCierre: Long,
    @SerializedName("hash_linea") val hashLinea: String,
    val productos: List<DetalleProductoCuentaExport>
)

data class DetalleProductoCuentaExport(val nombre: String, @SerializedName("codigo_barras") val codigoBarras: String, val cantidad: Int, @SerializedName("precio_unitario") val precioUnitario: Double, val subtotal: Double)
