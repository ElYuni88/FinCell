package com.tuapp.ventas.data.model

/** Archivo JSON exportado con el resumen diario IPB. */
data class ArchivoIPB(
    val fecha: String,
    val timestamp: Long,
    val puntoVenta: PuntoVentaExport? = null,
    val productos: List<ProductoIPB>,
    val gastos: List<Gasto> = emptyList(),
    val ingresos: List<Gasto> = emptyList(),
    val cuentas: List<CuentaIPB> = emptyList(),
    val resumenClientes: ResumenClientesIPB? = null,
    val resumen: ResumenIPB
)

/**
 * Producto incluido en el informe IPB con cálculos derivados de stock y subtotal.
 *
 * ⚠️ `inactivo` es el estado actual del producto en el cliente.
 *    El admin lo usa para saber si el cliente activó o desactivó algo durante el día.
 */
data class ProductoIPB(
    val id: Long,
    val nombre: String,
    val codigoBarras: String,
    val precio: Double,
    val inventario: Int,
    val vendidos: Int,
    val inactivo: Boolean = false
) {
    val stockFinal: Int get() = inventario - vendidos
    val subtotal: Double get() = vendidos * precio
}

/** Totales consolidados del IPB diario. */
data class ResumenIPB(
    val totalVentas: Double,
    val totalCuentas: Double,
    val totalGeneral: Double,
    val cantidadVentas: Int,
    val cantidadCuentas: Int,
    val totalGastos: Double = 0.0,
    val totalIngresos: Double = 0.0,
    val totalNeto: Double = totalGeneral + totalIngresos - totalGastos
)