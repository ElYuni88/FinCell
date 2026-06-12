package com.tuapp.ventas.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.tuapp.ventas.data.model.*
import com.tuapp.ventas.data.repository.VentasRepository
import java.io.File
import java.util.Calendar

data class ExportResult(val uri: Uri, val file: File?, val hashTotal: String)  // ← Cambiado: ahora puede ser URI o File

class FileExporter(private val context: Context, private val repository: VentasRepository) {

    suspend fun exportarVentasDelDia(time: Long = System.currentTimeMillis()): ExportResult {
        val inicio = DateUtils.inicioDia(time)
        val fin = DateUtils.finDia(time)
        val fecha = DateUtils.fechaArchivo(time)
        val ventas = repository.ventasDirectasDia(inicio, fin)
        val cuentas = repository.cuentasCerradasDia(inicio, fin)
        require(ventas.isNotEmpty() || cuentas.isNotEmpty()) { "No hay ventas para exportar" }

        // Obtener hash anterior (sin cambios)
        val dirLegacy = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "VentasSeguras").apply { mkdirs() }
        } else {
            null // En Android 10+, no usamos File system directamente
        }
        val hashAnterior = buscarHashAnterior(dirLegacy, time)

        // Calcular datos (sin cambios)
        val detalleVentas = ventas.map { DetalleVentaSimpleExport(it.id, it.codigoBarras, it.nombreProducto, it.precio, it.fechaVenta, HashValidator.hashVentaSimple(it.id, it.codigoBarras, it.precio, it.fechaVenta)) }
        val detalleCuentas = cuentas.map { cuenta ->
            DetalleCuentaExport(
                cuenta.cuenta.id,
                cuenta.cliente?.nombre ?: cuenta.cuenta.nombreClienteTemporal ?: "Cliente temporal",
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

        val json = GsonBuilder().setPrettyPrinting().create().toJson(archivo)
        val fileName = "ventas_$fecha.ventas"

        // 🔧 NUEVO: Guardar según versión de Android
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            guardarConMediaStore(fileName, json)
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "VentasSeguras").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeText(json)
            uriParaCompartir(file) // Retorna URI del FileProvider
        }

        return ExportResult(uri, null, hashTotal)
    }

    // 🔧 NUEVO: Guardar en Android 10+ usando MediaStore
    private fun guardarConMediaStore(fileName: String, content: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/VentasSeguras")
        }

        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: throw Exception("No se pudo crear el archivo en MediaStore")

        context.contentResolver.openOutputStream(uri).use { outputStream ->
            outputStream?.write(content.toByteArray())
                ?: throw Exception("No se pudo escribir el archivo")
        }
        return uri
    }

    // Método existente (modificado para aceptar URI)
    fun uriParaCompartir(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    fun uriParaCompartir(uri: Uri): Uri = uri // Si ya es URI, devolver igual

    private fun buscarHashAnterior(dir: File?, time: Long): String? {
        if (dir == null) return null // En Android 10+ no buscamos hash anterior por ahora
        val cal = Calendar.getInstance().apply { timeInMillis = time; add(Calendar.DAY_OF_MONTH, -1) }
        val anterior = File(dir, "ventas_${DateUtils.fechaArchivo(cal.timeInMillis)}.ventas")
        if (!anterior.exists()) return null
        return runCatching { GsonBuilder().create().fromJson(anterior.readText(), ArchivoVentas::class.java).hashTotal }.getOrNull()
    }
}