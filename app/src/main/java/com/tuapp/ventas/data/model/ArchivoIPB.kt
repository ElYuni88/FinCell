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
    val resumen: ResumenIPB,
    /**
     * ✅ NUEVO (v1.1 IPB): resumen de ventas por usuario.
     * Vacío hasta que la Fase 2 lo rellene. Admin debe aceptar el campo opcional.
     */
    val usuarios: List<UsuarioIPB> = emptyList()
)

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

/**
 * ✅ NUEVO: ventas agrupadas por usuario (quién vendió cuánto).
 * Permite al admin saber el desempeño por trabajador.
 */
data class UsuarioIPB(
    val usuarioId: Long,
    val usuarioNombre: String,
    val totalVentas: Double,
    val cantidadVentas: Int
)