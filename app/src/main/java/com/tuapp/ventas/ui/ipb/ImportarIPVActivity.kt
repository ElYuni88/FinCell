package com.tuapp.ventas.ui.ipb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ArchivoIPV
import com.tuapp.ventas.databinding.ActivityImportarIpbBinding
import com.tuapp.ventas.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class ImportarIPVActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportarIpbBinding
    private var archivoIPV: ArchivoIPV? = null
    private val repo by lazy { (application as VentasApplication).repository }

    private val seleccionarArchivoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
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
        supportActionBar?.title = "Importar IPV"

        binding.btnSeleccionarArchivo.setOnClickListener {
            seleccionarArchivoLauncher.launch("application/json")
        }

        binding.btnImportar.setOnClickListener {
            confirmarImportacion()
        }

        // Cambiar texto del botón
        binding.btnImportar.text = "Importar IPV"
    }

    private fun procesarArchivo(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = InputStreamReader(inputStream)
            archivoIPV = Gson().fromJson(reader, ArchivoIPV::class.java)
            reader.close()
            inputStream?.close()

            if (archivoIPV == null) {
                Toast.makeText(this, "Error al leer el archivo IPV", Toast.LENGTH_LONG).show()
                return
            }

            // Validar que tenga productos
            if (archivoIPV?.productos.isNullOrEmpty()) {
                Toast.makeText(this, "El archivo IPV no contiene productos", Toast.LENGTH_LONG).show()
                binding.btnImportar.isEnabled = false
                return
            }

            binding.txtArchivoSeleccionado.text = "Archivo: ${uri.lastPathSegment}"
            binding.txtFechaIPB.text = """
                PV: ${archivoIPV?.puntoVenta?.codigo ?: "--"} · ${archivoIPV?.puntoVenta?.nombre ?: ""}
                Fecha: ${DateUtils.fechaHora(archivoIPV?.fechaExportacion ?: 0)}
                Productos: ${archivoIPV?.productos?.size ?: 0}
            """.trimIndent()

            // Mostrar productos en el RecyclerView
            val adapter = IPBAdapter(
                archivoIPV?.productos?.map { productoIPV ->
                    // Convertir ProductoIPV a ProductoIPB para mostrar
                    com.tuapp.ventas.data.model.ProductoIPB(
                        id = 0, // Temporal, solo para mostrar
                        nombre = productoIPV.nombre,
                        codigoBarras = productoIPV.codigoBarras,
                        precio = productoIPV.precio,
                        inventario = productoIPV.inventario,
                        vendidos = 0
                    )
                } ?: emptyList()
            )
            binding.recyclerProductosIPB.setHasFixedSize(true)
            binding.recyclerProductosIPB.layoutManager = LinearLayoutManager(this)
            binding.recyclerProductosIPB.adapter = adapter

            binding.btnImportar.isEnabled = true
            Toast.makeText(this, "Archivo IPV cargado correctamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar el archivo: ${e.message}", Toast.LENGTH_LONG).show()
            archivoIPV = null
            binding.btnImportar.isEnabled = false
        }
    }

    private fun confirmarImportacion() {
        val archivo = archivoIPV ?: return
        val productos = archivo.productos

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar importación IPV")
            .setMessage("""
                Se importarán ${productos.size} productos.
                
                PV: ${archivo.puntoVenta.codigo} - ${archivo.puntoVenta.nombre}
                Fecha: ${DateUtils.fechaHora(archivo.fechaExportacion)}
                
                ¿Continuar?
            """.trimIndent())
            .setPositiveButton("Importar") { _, _ ->
                realizarImportacion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun realizarImportacion() {
        val archivo = archivoIPV ?: return

        if (archivo.productos.isEmpty()) {
            Toast.makeText(this, "No hay productos para importar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    repo.importarIPV(archivo)
                }

                withContext(Dispatchers.Main) {
                    if (resultado.exito) {
                        Toast.makeText(
                            this@ImportarIPVActivity,
                            resultado.mensaje,
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        MaterialAlertDialogBuilder(this@ImportarIPVActivity)
                            .setTitle("Error de validación")
                            .setMessage(resultado.mensaje)
                            .setPositiveButton("Entendido", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ImportarIPVActivity,
                        "Error al importar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
