package com.tuapp.ventas.data.model

/** Archivo JSON exportado con el resumen diario IPB. */
data class ArchivoIPB(
    val fecha: String,
    val timestamp: Long,
    val productos: List<ProductoIPB>,
    val gastos: List<Gasto> = emptyList(),
    val resumen: ResumenIPB
)

/** Producto incluido en el informe IPB con cálculos derivados de stock y subtotal. */
data class ProductoIPB(
    val id: Long,
    val nombre: String,
    val codigoBarras: String,
    val precio: Double,
    val inventario: Int,
    val vendidos: Int
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
    val totalNeto: Double = totalGeneral - totalGastos
)
