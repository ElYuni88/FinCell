package com.tuapp.ventas.ui.productos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ActivityProductosBinding
import com.tuapp.ventas.databinding.DialogAgregarProductoBinding
import com.tuapp.ventas.ui.exportar.ExportarIPBActivity
import com.tuapp.ventas.ui.estadisticas.EstadisticasActivity
import com.tuapp.ventas.ui.main.MainActivity
import com.tuapp.ventas.ui.scanner.BarcodeScannerActivity

/** Pantalla de gestión de productos con alta por escáner, alta manual, edición y eliminación. */
class ProductosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductosBinding
    private val viewModel: ProductosViewModel by viewModels { ProductosViewModelFactory((application as VentasApplication).repository) }
    private val adapter = ProductoAdapter(::mostrarDialogoEditar, ::confirmarEliminar)
    private var productosCompletos: List<Producto> = emptyList()

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val codigo = result.data?.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE).orEmpty()
        if (codigo.isNotBlank()) mostrarDialogoAgregar(codigoEscaneado = codigo)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirScanner() else toast("Permiso de cámara requerido")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurarRecycler()
        configurarClicks()
        observarDatos()
    }

    private fun configurarRecycler() = with(binding.recyclerProductos) {
        layoutManager = LinearLayoutManager(this@ProductosActivity)
        adapter = this@ProductosActivity.adapter
    }

    private fun configurarClicks() = with(binding) {
        btnAgregarEscaneo.setOnClickListener { solicitarCamara() }
        btnAgregarManual.setOnClickListener { mostrarDialogoAgregar(codigoEscaneado = null) }
        btnVolver.setOnClickListener { finish() }
        bottomNavigation.selectedItemId = R.id.nav_productos
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan -> { startActivity(Intent(this@ProductosActivity, MainActivity::class.java)); true }
                R.id.nav_productos -> true
                R.id.nav_estadisticas -> { startActivity(Intent(this@ProductosActivity, EstadisticasActivity::class.java)); true }
                R.id.nav_exportar_ipb -> { confirmarExportarIPB(); true }
                else -> false
            }
        }
        inputBuscarProductos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = filtrarProductos(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observarDatos() {
        viewModel.productos.observe(this) { productos -> productosCompletos = productos.sortedBy { it.nombre.lowercase() }; filtrarProductos(binding.inputBuscarProductos.text?.toString().orEmpty()) }
        viewModel.mensaje.observe(this) { toast(it) }
    }

    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) abrirScanner()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun abrirScanner() = scanLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))

    private fun mostrarDialogoAgregar(codigoEscaneado: String?) {
        mostrarDialogoProducto(
            titulo = if (codigoEscaneado == null) "Agregar producto manual" else "Agregar producto escaneado",
            producto = null,
            codigoInicial = codigoEscaneado.orEmpty(),
            codigoEditable = codigoEscaneado == null
        )
    }

    private fun mostrarDialogoEditar(producto: Producto) {
        mostrarDialogoProducto(
            titulo = "Editar producto",
            producto = producto,
            codigoInicial = producto.codigoBarras,
            codigoEditable = false
        )
    }

    private fun mostrarDialogoProducto(titulo: String, producto: Producto?, codigoInicial: String, codigoEditable: Boolean) {
        val dialogBinding = DialogAgregarProductoBinding.inflate(layoutInflater)
        dialogBinding.inputCodigo.setText(codigoInicial)
        dialogBinding.inputCodigo.isEnabled = codigoEditable
        dialogBinding.inputNombre.setText(producto?.nombre.orEmpty())
        dialogBinding.inputPrecio.setText(producto?.precio?.toString().orEmpty())
        dialogBinding.inputInventario.setText((producto?.inventario ?: 0).toString())

        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val inventario = dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 0
                val codigo = dialogBinding.inputCodigo.text?.toString()?.trim().orEmpty()
                    .ifBlank { if (producto == null) viewModel.generarCodigoManualSugerido() else producto.codigoBarras }

                if (nombre.isBlank() || precio == null || precio < 0.0 || inventario < 0) {
                    toast("Nombre, precio e inventario válidos son requeridos")
                    return@setPositiveButton
                }

                val tipo = if (codigo.startsWith("MANUAL_", ignoreCase = true)) Producto.TIPO_MANUAL else Producto.TIPO_CODIGO_BARRAS
                val base = producto ?: Producto(nombre = nombre, precio = precio)
                viewModel.guardar(base.copy(nombre = nombre, codigoBarras = codigo, precio = precio, inventario = inventario, tipoProducto = producto?.tipoProducto ?: tipo))
            }
            .show()
    }

    private fun confirmarEliminar(producto: Producto) {
        val advertenciaVentas = if (producto.vendidos > 0) "\n\nAdvertencia: tiene ${producto.vendidos} ventas asociadas." else ""
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Eliminar ${producto.nombre}?$advertenciaVentas")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminar(producto) }
            .show()
    }

    private fun filtrarProductos(query: String) {
        val normalizada = query.trim().lowercase()
        val filtrados = if (normalizada.isBlank()) productosCompletos else productosCompletos.filter {
            it.nombre.lowercase().contains(normalizada) || it.codigoBarras.lowercase().contains(normalizada)
        }
        adapter.submitList(filtrados)
    }

    private fun confirmarExportarIPB() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Exportar IPB")
            .setMessage("¿Exportar las operaciones del día en formato IPB?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Exportar") { _, _ -> startActivity(Intent(this, ExportarIPBActivity::class.java)) }
            .show()
    }

    private fun toast(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
}
