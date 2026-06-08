package com.tuapp.ventas.utils

import android.content.Context
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.tuapp.ventas.data.model.*
import com.tuapp.ventas.data.repository.VentasRepository
import java.io.File
import java.util.Calendar

data class ExportResult(val file: File, val hashTotal: String)

class FileExporter(private val context: Context, private val repository: VentasRepository) {
    suspend fun exportarVentasDelDia(time: Long = System.currentTimeMillis()): ExportResult {
        val inicio = DateUtils.inicioDia(time)
        val fin = DateUtils.finDia(time)
        val fecha = DateUtils.fechaArchivo(time)
        val ventas = repository.ventasDirectasDia(inicio, fin)
        val cuentas = repository.cuentasCerradasDia(inicio, fin)
        require(ventas.isNotEmpty() || cuentas.isNotEmpty()) { "No hay ventas para exportar" }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "VentasSeguras").apply { mkdirs() }
        val hashAnterior = buscarHashAnterior(dir, time)
        val detalleVentas = ventas.map { DetalleVentaSimpleExport(it.id, it.codigoBarras, it.nombreProducto, it.precio, it.fechaVenta, HashValidator.hashVentaSimple(it.id, it.codigoBarras, it.precio, it.fechaVenta)) }
        val detalleCuentas = cuentas.map { cuenta ->
            DetalleCuentaExport(
                cuenta.cuenta.id,
                cuenta.cliente.nombre,
                cuenta.cuenta.total,
                cuenta.cuenta.fechaCierre ?: time,
                HashValidator.hashCuenta(cuenta.cuenta.id, cuenta.cuenta.total, cuenta.cuenta.fechaCierre ?: time),
                cuenta.detalles.map { DetalleProductoCuentaExport(it.producto.nombre, it.producto.codigoBarras, it.detalle.cantidad, it.detalle.precioUnitario, it.detalle.subtotal) }
            )
        }
        val cantidades = mutableMapOf<String, Int>()
        ventas.forEach { cantidades[it.nombreProducto] = (cantidades[it.nombreProducto] ?: 0) + 1 }
        cuentas.flatMap { it.detalles }.forEach { cantidades[it.producto.nombre] = (cantidades[it.producto.nombre] ?: 0) + it.detalle.cantidad }
        val top = cantidades.maxByOrNull { it.value }?.let { ProductoMasVendido(it.key, it.value) }
        val resumen = ResumenVentas(
            ventas.sumOf { it.precio }, cuentas.sumOf { it.cuenta.total }, ventas.sumOf { it.precio } + cuentas.sumOf { it.cuenta.total }, ventas.size, cuentas.size, top
        )
        val hashes = detalleVentas.map { it.hashLinea } + detalleCuentas.map { it.hashLinea }
        val hashTotal = HashValidator.hashTotal(hashes, hashAnterior, fecha)
        val archivo = ArchivoVentas(fecha = fecha, timestampGeneracion = System.currentTimeMillis(), hashAnterior = hashAnterior, resumen = resumen, detalleVentasSimples = detalleVentas, detalleCuentas = detalleCuentas, hashTotal = hashTotal)
        val file = File(dir, "ventas_$fecha.ventas")
        file.writeText(GsonBuilder().setPrettyPrinting().create().toJson(archivo))
        return ExportResult(file, hashTotal)
    }

    fun uriParaCompartir(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun buscarHashAnterior(dir: File, time: Long): String? {
        val cal = Calendar.getInstance().apply { timeInMillis = time; add(Calendar.DAY_OF_MONTH, -1) }
        val anterior = File(dir, "ventas_${DateUtils.fechaArchivo(cal.timeInMillis)}.ventas")
        if (!anterior.exists()) return null
        return runCatching { GsonBuilder().create().fromJson(anterior.readText(), ArchivoVentas::class.java).hashTotal }.getOrNull()
    }
}
