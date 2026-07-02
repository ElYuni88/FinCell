package com.tuapp.ventas.ui.exportar

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.tuapp.ventas.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ArchivoIPB
import com.tuapp.ventas.data.model.ProductoIPB
import com.tuapp.ventas.data.model.ResumenIPB
import com.tuapp.ventas.utils.DateUtils
import kotlinx.coroutines.launch
import java.io.File

class ExportarIPBActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exportar()
    }

    private fun exportar() = lifecycleScope.launch {
        runCatching {
            val repo = (application as VentasApplication).repository
            val now = System.currentTimeMillis()
            val inicio = DateUtils.inicioDia(now)
            val fin = DateUtils.finDia(now)

            val productos = repo.listarProductos().map {
                ProductoIPB(
                    id = it.id,
                    nombre = it.nombre,
                    codigoBarras = it.codigoBarras,
                    precio = it.precio,
                    inventario = it.inventario,
                    vendidos = it.vendidos
                )
            }

            val ventas = repo.ventasDirectasDia(inicio, fin)
            val cuentas = repo.cuentasCerradasDia(inicio, fin)

            val archivo = ArchivoIPB(
                fecha = DateUtils.fechaArchivo(now),
                timestamp = now,
                productos = productos,
                resumen = ResumenIPB(
                    totalVentas = ventas.sumOf { it.precio },
                    totalCuentas = cuentas.sumOf { it.cuenta.total },
                    totalGeneral = ventas.sumOf { it.precio } + cuentas.sumOf { it.cuenta.total },
                    cantidadVentas = ventas.size,
                    cantidadCuentas = cuentas.size
                )
            )

            val json = GsonBuilder().setPrettyPrinting().create().toJson(archivo)
            val nombre = "ipb_${archivo.fecha}.json"
            guardar(nombre, json)
        }.onSuccess {
            Toast.makeText(
                this@ExportarIPBActivity,
                "IPB exportado en Descargas",
                Toast.LENGTH_LONG
            ).show()
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

    private fun guardar(nombre: String, json: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("No se pudo crear IPB")
            contentResolver.openOutputStream(uri).use { outputStream ->
                outputStream?.write(json.toByteArray()) ?: error("No se pudo escribir IPB")
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            File(dir, nombre).writeText(json)
        }
    }
}