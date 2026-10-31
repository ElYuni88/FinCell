package com.tuapp.ventas.data.model

/**
 * Modelo de análisis completo del período para la pantalla "Análisis del período".
 *
 * Por cada producto incluye todos los datos necesarios para verificar el
 * resultado del período antes de exportar el IPB:
 *
 * - stockInicial    → inventarioAlInicioPeriodo (lo que tenía al importar el IPV)
 * - entrada         → inventario actual - inventarioAlInicioPeriodo
 *                     (stock agregado manualmente durante el período)
 * - vendidosTotal   → producto.vendidos (acumulado: período muerto + actual)
 * - vendidosMuerto  → ventas hechas en el período muerto inmediatamente anterior
 * - vendidosPeriodo → vendidosTotal - vendidosMuerto
 * - stockFinal      → inventario - vendidosTotal
 * - subtotal        → vendidosTotal * precio
 */
data class ProductoAnalisis(
    val id: Long,
    val codigoBarras: String,
    val nombre: String,
    val precio: Double,
    val stockInicial: Int,
    val entrada: Int,
    val vendidosTotal: Int,
    val vendidosMuerto: Int,
    val vendidosPeriodo: Int,
    val stockFinal: Int,
    val subtotal: Double,
    val inactivo: Boolean
)