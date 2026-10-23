package com.tuapp.ventas.ui.exportar

import android.content.ContentUris
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ArchivoIPB
import com.tuapp.ventas.data.model.ClienteRecurrenteIPB
import com.tuapp.ventas.data.model.CuentaIPB
import com.tuapp.ventas.data.model.EstadoPeriodo
import com.tuapp.ventas.data.model.Gasto
import com.tuapp.ventas.data.model.ProductoCuentaIPB
import com.tuapp.ventas.data.model.ProductoIPB
import com.tuapp.ventas.data.model.ResumenClientesIPB
import com.tuapp.ventas.data.model.ResumenIPB
import com.tuapp.ventas.data.model.toPuntoVentaExport
import com.tuapp.ventas.ui.base.BaseActivity
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream

class ExportarIPBActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sobrescribir = intent.getBooleanExtra(EXTRA_SOBRESCRIBIR, false)
        exportar(sobrescribir)
    }

    private fun exportar(sobrescribir: Boolean) = lifecycleScope.launch {
        var nombreArchivo = ""

        runCatching {
            val repo = (application as VentasApplication).repository
            val prefs = PreferencesManager(this@ExportarIPBActivity)

            // ✅ RANGO: desde inicio del periodo hasta ahora
            val inicioPeriodo = prefs.obtenerInicioPeriodoVentas()
            val inicio = if (inicioPeriodo > 0L) {
                inicioPeriodo
            } else {
                DateUtils.inicioDia(System.currentTimeMillis())
            }
            val fin = System.currentTimeMillis()
            val fechaStr = DateUtils.fechaArchivo(fin)

            Log.d(
                "ExportarIPB",
                "Exportando período: inicio=${DateUtils.fechaHora(inicio)} fin=${DateUtils.fechaHora(fin)}"
            )

            val pv = repo.obtenerPuntoVentaActivo()
            val pvExport = pv?.toPuntoVentaExport()

            val cuentasCerradas = repo.cuentasCerradasDia(inicio, fin)

            val productos = repo.listarProductos().map {
                ProductoIPB(
                    id = it.id,
                    nombre = it.nombre,
                    codigoBarras = it.codigoBarras,
                    precio = it.precio,
                    inventario = it.inventario,
                    vendidos = it.vendidos,
                    inactivo = it.inactivo
                )
            }

            val ventas = repo.ventasDirectasDia(inicio, fin)

            // ✅ Gastos e ingresos de TODO el rango del periodo
            val gastos = prefs.obtenerGastosActivosRango(inicio, fin)
            val ingresos = prefs.obtenerIngresosActivosRango(inicio, fin)

            val cuentasIPB = cuentasCerradas.map { cuentaConDetalles ->
                val cuenta = cuentaConDetalles.cuenta
                val cliente = cuentaConDetalles.cliente

                val ventaFinal = repo.obtenerVentaFinal(cuenta.id)
                val metodoPago = ventaFinal?.metodoPago ?: "EFECTIVO"

                CuentaIPB(
                    cuentaId = cuenta.id,
                    clienteId = cuenta.clienteId,
                    nombreCliente = cliente?.nombre
                        ?: cuenta.nombreClienteTemporal
                        ?: "Cliente temporal",
                    telefono = cliente?.telefono,
                    mesa = cliente?.mesa ?: cuenta.mesaTemporal,
                    esClienteTemporal = cuenta.esClienteTemporal,
                    fechaApertura = cuenta.fechaApertura,
                    fechaCierre = cuenta.fechaCierre ?: cuenta.fechaApertura,
                    total = cuenta.total,
                    metodoPago = metodoPago,
                    cantidadProductos = cuentaConDetalles.detalles.sumOf { it.detalle.cantidad },
                    productos = cuentaConDetalles.detalles.map { detalleConProducto ->
                        ProductoCuentaIPB(
                            nombre = detalleConProducto.producto.nombre,
                            codigoBarras = detalleConProducto.producto.codigoBarras,
                            cantidad = detalleConProducto.detalle.cantidad,
                            precioUnitario = detalleConProducto.detalle.precioUnitario,
                            subtotal = detalleConProducto.detalle.subtotal
                        )
                    }
                )
            }

            val totalVentas = ventas.sumOf { it.precio }
            val totalCuentas = cuentasCerradas.sumOf { it.cuenta.total }
            val totalGeneral = totalVentas + totalCuentas
            val totalGastos = gastos.sumOf { it.monto }
            val totalIngresos = ingresos.sumOf { it.monto }
            val resumenClientes = calcularResumenClientes(cuentasIPB)

            val archivo = ArchivoIPB(
                fecha = fechaStr,   // Fecha del cierre (hoy). El rango se infiere por productos[].vendidos
                timestamp = System.currentTimeMillis(),
                puntoVenta = pvExport,
                productos = productos,
                gastos = gastos,
                ingresos = ingresos,
                cuentas = cuentasIPB,
                resumenClientes = resumenClientes,
                resumen = ResumenIPB(
                    totalVentas = totalVentas,
                    totalCuentas = totalCuentas,
                    totalGeneral = totalGeneral,
                    cantidadVentas = ventas.size,
                    cantidadCuentas = cuentasCerradas.size,
                    totalGastos = totalGastos,
                    totalIngresos = totalIngresos,
                    totalNeto = totalGeneral + totalIngresos - totalGastos
                )
            )

            val json = Gson().toJson(archivo)
            val bytesComprimidos = comprimirConGzip(json)

            nombreArchivo = "ipb_${fechaStr}.ipb"
            guardarBinario(nombreArchivo, bytesComprimidos, sobrescribir)

            Log.d(
                "ExportarIPB",
                "Fecha cierre: $fechaStr | Ventas: ${ventas.size} | Cuentas: ${cuentasCerradas.size} | Original: ${json.toByteArray().size} b | Comprimido: ${bytesComprimidos.size} b"
            )
        }.onSuccess {
            val prefs = PreferencesManager(this@ExportarIPBActivity)
            prefs.guardarUltimaExportacionIPB(System.currentTimeMillis())
            prefs.nombreArchivoExportado = nombreArchivo

            // ✅ Cerrar el periodo con la fecha de HOY
            val repo = (application as VentasApplication).repository
            val resultado = repo.cerrarPeriodo(System.currentTimeMillis())

            if (resultado.exito) {
                Toast.makeText(
                    this@ExportarIPBActivity,
                    "🔒 Periodo cerrado y exportado\n\n" +
                            "Esperando el IPV del admin para reabrir.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@ExportarIPBActivity,
                    "⚠️ IPB exportado, pero no se pudo cerrar el periodo:\n${resultado.mensaje}",
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        }.onFailure { e ->
            Toast.makeText(
                this@ExportarIPBActivity,
                e.message ?: "Error al exportar IPB",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun calcularResumenClientes(cuentas: List<CuentaIPB>): ResumenClientesIPB {
        val porCliente = cuentas.groupBy { it.clienteId to it.nombreCliente }

        val clientesRecurrentes = porCliente.map { (key, cuentasCliente) ->
            val clienteId = key.first
            val nombre = key.second
            val totalConsumido = cuentasCliente.sumOf { it.total }
            val cantidadCuentas = cuentasCliente.size
            val ticketPromedio = if (cantidadCuentas > 0) totalConsumido / cantidadCuentas else 0.0
            val metodoPagoFrecuente = cuentasCliente
                .groupBy { it.metodoPago }
                .maxByOrNull { it.value.size }
                ?.key ?: "EFECTIVO"

            ClienteRecurrenteIPB(
                clienteId = clienteId,
                nombreCliente = nombre,
                cantidadCuentas = cantidadCuentas,
                totalConsumido = totalConsumido,
                ticketPromedio = ticketPromedio,
                metodoPagoFrecuente = metodoPagoFrecuente
            )
        }.sortedByDescending { it.totalConsumido }

        val recurrentes = clientesRecurrentes.filter { it.cantidadCuentas > 1 }

        return ResumenClientesIPB(
            totalClientes = porCliente.size,
            clientesRecurrentes = recurrentes,
            mejorCliente = clientesRecurrentes.firstOrNull()
        )
    }

    private fun comprimirConGzip(texto: String): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(texto.toByteArray(Charsets.UTF_8))
        }
        return baos.toByteArray()
    }

    private fun guardarBinario(nombre: String, bytes: ByteArray, sobrescribir: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(nombre)

            val cursor = contentResolver.query(collection, projection, selection, selectionArgs, null)
            val id = cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                } else null
            }

            if (id != null) {
                if (sobrescribir) {
                    val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        outputStream?.write(bytes) ?: error("No se pudo escribir IPB")
                    }
                    return
                } else {
                    Toast.makeText(this, "El archivo ya existe. Usa la opción de sobrescritura.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("No se pudo crear IPB")
            contentResolver.openOutputStream(uri).use { outputStream ->
                outputStream?.write(bytes) ?: error("No se pudo escribir IPB")
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val archivo = File(dir, nombre)
            if (archivo.exists()) {
                if (sobrescribir) {
                    archivo.delete()
                } else {
                    Toast.makeText(this, "El archivo ya existe. Usa la opción de sobrescritura.", Toast.LENGTH_LONG).show()
                    return
                }
            }
            archivo.writeBytes(bytes)
        }
    }

    companion object {
        const val EXTRA_SOBRESCRIBIR = "extra_sobrescribir"
        const val EXTRA_FECHA_SELECCIONADA = "extra_fecha_seleccionada"   // mantenido por compatibilidad, ya no se usa
    }
}