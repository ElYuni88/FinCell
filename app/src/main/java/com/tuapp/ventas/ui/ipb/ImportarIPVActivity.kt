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
            seleccionarArchivoLauncher.launch("*/*")
        }

        binding.btnImportar.setOnClickListener {
            confirmarImportacion()
        }

        // Cambiar texto del botón
        binding.btnImportar.text = "Importar IPV"
    }

    private fun procesarArchivo(uri: Uri) {
        lifecycleScope.launch {
            try {
                binding.btnImportar.isEnabled = false
                binding.txtArchivoSeleccionado.text = "Leyendo archivo…"

                val archivo = withContext(Dispatchers.IO) {
                    com.tuapp.ventas.utils.IpvReader.leerDesdeUri(this@ImportarIPVActivity, uri)
                }
                archivoIPV = archivo

                // Mostrar info del archivo
                val nombre = uri.lastPathSegment ?: "ipv_desconocido.ipv"
                binding.txtArchivoSeleccionado.text = "Archivo: $nombre"
                binding.txtFechaIPB.text = """
                PV: ${archivo.puntoVenta.codigo} · ${archivo.puntoVenta.nombre}
                Fecha exportación: ${DateUtils.fechaHora(archivo.fechaExportacion)}
                Productos: ${archivo.productos.size}
                Tipo: ${if (archivo.esSincronizacionInicial) "Sincronización inicial" else "Actualización"}
            """.trimIndent()

                // Mostrar productos en el RecyclerView
                val adapter = IPBAdapter(
                    archivo.productos.map { productoIPV ->
                        com.tuapp.ventas.data.model.ProductoIPB(
                            id = 0,
                            nombre = productoIPV.nombre,
                            codigoBarras = productoIPV.codigoBarras,
                            precio = productoIPV.precio,
                            inventario = productoIPV.inventario,
                            vendidos = 0
                        )
                    }
                )
                binding.recyclerProductosIPB.setHasFixedSize(true)
                binding.recyclerProductosIPB.layoutManager = LinearLayoutManager(this@ImportarIPVActivity)
                binding.recyclerProductosIPB.adapter = adapter

                binding.btnImportar.isEnabled = true
                Toast.makeText(this@ImportarIPVActivity, "Archivo IPV cargado", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ImportarIPVActivity,
                    "Error al procesar el archivo: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                archivoIPV = null
                binding.btnImportar.isEnabled = false
            }
        }
    }

    private fun confirmarImportacion() {
        val archivo = archivoIPV ?: return
        val productos = archivo.productos

        val tipoMensaje = if (archivo.esSincronizacionInicial) {
            if (productos.isEmpty()) {
                "Se creará el Punto de Venta ${archivo.puntoVenta.codigo} - ${archivo.puntoVenta.nombre}\n" +
                        "Sin productos (solo sincronización del PV)."
            } else {
                "Se creará el Punto de Venta e importarán ${productos.size} productos."
            }
        } else {
            "Se actualizarán ${productos.size} productos."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar importación IPV")
            .setMessage("""
            $tipoMensaje
            
            PV: ${archivo.puntoVenta.codigo} - ${archivo.puntoVenta.nombre}
            Fecha: ${DateUtils.fechaHora(archivo.fechaExportacion)}
            Tipo: ${if (archivo.esSincronizacionInicial) "Sincronización inicial" else "Actualización"}
            
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
        val nombreArchivo = binding.txtArchivoSeleccionado.text.toString().removePrefix("Archivo: ")

        // Validación: solo exigir productos si NO es sincronización inicial
        if (!archivo.esSincronizacionInicial && archivo.productos.isEmpty()) {
            Toast.makeText(
                this,
                "Este archivo de actualización no contiene productos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    repo.importarIPV(archivo, nombreArchivo)
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
