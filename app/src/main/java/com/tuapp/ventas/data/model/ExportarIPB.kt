package com.tuapp.ventas.data.model

data class ArchivoIPB(val fecha: String, val timestamp: Long, val productos: List<ProductoIPB>, val resumen: ResumenIPB)
data class ProductoIPB(val id: Long, val nombre: String, val codigoBarras: String, val precio: Double, val inventario: Int, val vendidos: Int)
data class ResumenIPB(val totalVentas: Double, val totalCuentas: Double, val totalGeneral: Double, val cantidadVentas: Int, val cantidadCuentas: Int)
