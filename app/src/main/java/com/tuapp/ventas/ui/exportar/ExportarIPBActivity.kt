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
import com.tuapp.ventas.utils.PreferencesManager
import kotlinx.coroutines.launch
import java.io.File

class ExportarIPBActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sobrescribir = intent.getBooleanExtra(EXTRA_SOBRESCRIBIR, false)
        exportar(sobrescribir)
    }

    private fun exportar(sobrescribir: Boolean) = lifecycleScope.launch {
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
            val gastos = PreferencesManager(this@ExportarIPBActivity).obtenerGastos(DateUtils.fechaArchivo(now))
            val totalVentas = ventas.sumOf { it.precio }
            val totalCuentas = cuentas.sumOf { it.cuenta.total }
            val totalGeneral = totalVentas + totalCuentas
            val totalGastos = gastos.sumOf { it.monto }

            val archivo = ArchivoIPB(
                fecha = DateUtils.fechaArchivo(now),
                timestamp = now,
                productos = productos,
                gastos = gastos,
                resumen = ResumenIPB(
                    totalVentas = totalVentas,
                    totalCuentas = totalCuentas,
                    totalGeneral = totalGeneral,
                    cantidadVentas = ventas.size,
                    cantidadCuentas = cuentas.size,
                    totalGastos = totalGastos,
                    totalNeto = totalGeneral - totalGastos
                )
            )

            val json = GsonBuilder().setPrettyPrinting().create().toJson(archivo)
            val nombre = "ipb_${archivo.fecha}.json"
            guardar(nombre, json, sobrescribir)
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

    private fun guardar(nombre: String, json: String, sobrescribir: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Para Android 10+, usar MediaStore con posibilidad de sobrescritura
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(nombre)

            // Buscar si ya existe un archivo con ese nombre
            val cursor = contentResolver.query(collection, projection, selection, selectionArgs, null)
            val id = cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                } else null
            }

            if (id != null) {
                if (sobrescribir) {
                    // Eliminar el existente
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }.let {
                        contentResolver.update(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            it,
                            "${MediaStore.MediaColumns._ID} = ?",
                            arrayOf(id.toString())
                        )
                    }
                    // Ahora eliminar
                    contentResolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns._ID} = ?",
                        arrayOf(id.toString())
                    )
                } else {
                    // No sobrescribir, mostrar mensaje
                    Toast.makeText(
                        this,
                        "El archivo ya existe. Usa la opción de sobrescritura.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return
                }
            }

            // Insertar el nuevo archivo
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
            // Android 9 o inferior
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val archivo = File(dir, nombre)

            if (archivo.exists()) {
                if (sobrescribir) {
                    archivo.delete()
                } else {
                    Toast.makeText(
                        this,
                        "El archivo ya existe. Usa la opción de sobrescritura.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return
                }
            }
            archivo.writeText(json)
        }
    }

    companion object {
        const val EXTRA_SOBRESCRIBIR = "extra_sobrescribir"
    }
}