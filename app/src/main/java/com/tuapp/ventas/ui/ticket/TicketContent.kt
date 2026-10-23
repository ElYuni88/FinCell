package com.tuapp.ventas.ui.ticket

import com.tuapp.ventas.data.model.VentaFinal
import com.tuapp.ventas.data.model.relaciones.CuentaConDetalles
import com.tuapp.ventas.utils.DateUtils

data class TicketProducto(
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

data class TicketData(
    val cuentaId: Long,
    val nombreCliente: String,
    val mesa: String,
    val fechaApertura: Long,
    val fechaCierre: Long?,
    val metodoPago: String,
    val observaciones: String?,
    val productos: List<TicketProducto>,
    val total: Double,
    val nombrePuntoVenta: String
)

object TicketContent {

    fun construirDatos(
        cuenta: CuentaConDetalles,
        ventaFinal: VentaFinal?,
        nombrePuntoVenta: String,
        nombreCliente: String,
        mesa: String
    ): TicketData = TicketData(
        cuentaId = cuenta.cuenta.id,
        nombreCliente = nombreCliente,
        mesa = mesa,
        fechaApertura = cuenta.cuenta.fechaApertura,
        fechaCierre = cuenta.cuenta.fechaCierre,
        metodoPago = ventaFinal?.metodoPago ?: "No especificado",
        observaciones = ventaFinal?.observaciones,
        productos = cuenta.detalles.map {
            TicketProducto(
                nombre = it.producto.nombre,
                cantidad = it.detalle.cantidad,
                precioUnitario = it.detalle.precioUnitario,
                subtotal = it.detalle.subtotal
            )
        },
        total = cuenta.cuenta.total,
        nombrePuntoVenta = nombrePuntoVenta
    )

    /** Texto plano compatible con impresoras térmicas (80 columnas). */
    fun textoPlano(data: TicketData): String = buildString {
        appendLine("==================================================")
        appendLine("                    CafePos")
        appendLine("        ¡Bienvenidos a ${data.nombrePuntoVenta}!")
        appendLine("==================================================")
        appendLine()
        appendLine("Cuenta: #${data.cuentaId}")
        appendLine("Cliente: ${data.nombreCliente}")
        appendLine("Mesa: ${data.mesa}")
        appendLine("Apertura: ${DateUtils.fechaHora(data.fechaApertura)}")
        data.fechaCierre?.let { appendLine("Cierre: ${DateUtils.fechaHora(it)}") }
        appendLine("Pago: ${data.metodoPago}")
        if (!data.observaciones.isNullOrBlank()) {
            appendLine("Obs: ${data.observaciones}")
        }
        appendLine("--------------------------------------------------")
        appendLine(String.format("%-24s %6s %14s", "PRODUCTO", "CANT", "SUBTOTAL"))
        appendLine("--------------------------------------------------")
        data.productos.forEach { p ->
            // 1) Nombre completo (sin truncar), wrap a 50 chars si hace falta
            wrapTexto(p.nombre, 50).forEach { appendLine(it) }

            // 2) Detalle alineado a la derecha
            val detalle = if (p.cantidad > 1) {
                "${p.cantidad} × ${DateUtils.moneda(p.precioUnitario)} = ${DateUtils.moneda(p.subtotal)}"
            } else {
                DateUtils.moneda(p.subtotal)
            }
            appendLine(String.format("%50s", detalle))
        }
        appendLine("--------------------------------------------------")
        appendLine(String.format("%38s %11s", "TOTAL:", DateUtils.moneda(data.total)))
        appendLine()
        appendLine("        ¡Gracias por su preferencia!")
        appendLine("==================================================")
    }

    /** Corta un texto en líneas de máximo `ancho` caracteres, sin romper palabras. */
    private fun wrapTexto(texto: String, ancho: Int): List<String> {
        if (texto.length <= ancho) return listOf(texto)
        val lineas = mutableListOf<String>()
        var resto = texto
        while (resto.length > ancho) {
            // Busca el último espacio antes del ancho para no cortar palabras
            val corte = resto.lastIndexOf(' ', ancho).takeIf { it > 0 } ?: ancho
            lineas.add(resto.substring(0, corte).trimEnd())
            resto = resto.substring(corte).trimStart()
        }
        if (resto.isNotEmpty()) lineas.add(resto)
        return lineas
    }
}