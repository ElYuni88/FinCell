package com.tuapp.ventas.ui.ipb

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ArchivoIPB
import com.tuapp.ventas.databinding.ActivityImportarIpbBinding
import com.tuapp.ventas.utils.DateUtils
import java.io.InputStreamReader

class ImportarIPBActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportarIpbBinding
    private var archivoIPB: ArchivoIPB? = null
    private val repo by lazy { (application as VentasApplication).repository }

    private val seleccionarArchivoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            procesarArchivo(uri)
        } else {
            Toast.makeText(this, "No se seleccionó ningún archivo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportarIpbBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSeleccionarArchivo.setOnClickListener {
            seleccionarArchivoLauncher.launch("application/json")
        }

        binding.btnImportar.setOnClickListener {
            confirmarImportacion()
        }
    }

    private fun procesarArchivo(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = InputStreamReader(inputStream)
            archivoIPB = Gson().fromJson(reader, ArchivoIPB::class.java)
            reader.close()
            inputStream?.close()

            if (archivoIPB == null) {
                Toast.makeText(this, "Error al leer el archivo", Toast.LENGTH_LONG).show()
                return
            }

            val fechaArchivo = archivoIPB?.fecha ?: ""
            val fechaHoy = DateUtils.fechaArchivo(System.currentTimeMillis())

            if (fechaArchivo == fechaHoy) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Advertencia")
                    .setMessage("No se puede importar un archivo del día actual. El IPB del día actual solo puede ser exportado, no importado.")
                    .setPositiveButton("Entendido", null)
                    .show()
                binding.btnImportar.isEnabled = false
                binding.txtArchivoSeleccionado.text = "Archivo: ${uri.lastPathSegment} (no se puede importar hoy)"
                return
            }

            binding.txtArchivoSeleccionado.text = "Archivo: ${uri.lastPathSegment}"
            binding.txtFechaIPB.text = "Fecha IPB: ${archivoIPB?.fecha ?: "--"}"

            val adapter = IPBAdapter(archivoIPB?.productos ?: emptyList())
            binding.recyclerProductosIPB.layoutManager = LinearLayoutManager(this)
            binding.recyclerProductosIPB.adapter = adapter

            binding.btnImportar.isEnabled = true
            Toast.makeText(this, "Archivo cargado correctamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar el archivo: ${e.message}", Toast.LENGTH_LONG).show()
            archivoIPB = null
            binding.btnImportar.isEnabled = false
        }
    }

    private fun confirmarImportacion() {
        val archivo = archivoIPB ?: return
        val productos = archivo.productos

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar importación")
            .setMessage("Se actualizará el stock de ${productos.size} productos.\n\nFecha: ${archivo.fecha}\nTotal neto: ${DateUtils.moneda(archivo.resumen.totalNeto)}\n\n¿Continuar?")
            .setPositiveButton("Importar") { _, _ ->
                realizarImportacion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun realizarImportacion() {
        val archivo = archivoIPB ?: return
        val productos = archivo.productos

        if (productos.isEmpty()) {
            Toast.makeText(this, "No hay productos para importar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                productos.forEach { productoIPB ->
                    repo.actualizarStockDesdeIPB(productoIPB)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImportarIPBActivity, "Importación completada. Stock actualizado.", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImportarIPBActivity, "Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validarFechaIPB(fechaIPB: String): Boolean {
        // La fecha del IPB viene en formato "YYYY-MM-DD" (ej. "2026-07-29")
        // La fecha actual la obtenemos con DateUtils.fechaArchivo(System.currentTimeMillis())
        val fechaActual = DateUtils.fechaArchivo(System.currentTimeMillis())
        return fechaIPB != fechaActual
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}