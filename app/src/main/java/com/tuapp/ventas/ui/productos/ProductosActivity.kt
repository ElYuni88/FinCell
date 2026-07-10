package com.tuapp.ventas.ui.productos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.tuapp.ventas.ui.base.BaseActivity
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
class ProductosActivity : BaseActivity() {
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
        binding.toolbar.visibility = View.GONE
        binding.bottomNavigation.visibility = View.GONE
        configurarRecycler()
        configurarClicks()
        observarDatos()
    }

    private fun configurarRecycler() = with(binding.recyclerProductos) {
        layoutManager = LinearLayoutManager(this@ProductosActivity)
        adapter = this@ProductosActivity.adapter
    }

    private fun configurarClicks() = with(binding) {
        toolbar.setNavigationOnClickListener { startActivity(Intent(this@ProductosActivity, MainActivity::class.java)) }
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

    private fun mostrarDialogoAgregar(codigoEscaneado: String?, esManual: Boolean = true) {
        mostrarDialogoProducto(
            titulo = if (codigoEscaneado == null) "Agregar producto manual" else "Agregar producto escaneado",
            producto = null,
            codigoInicial = codigoEscaneado.orEmpty(),
            codigoEditable = codigoEscaneado == null,
            esManualForzado = esManual
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

    private fun mostrarDialogoProducto(
        titulo: String,
        producto: Producto?,
        codigoInicial: String,
        codigoEditable: Boolean,
        esManualForzado: Boolean = false
    ) {
        val dialogBinding = DialogAgregarProductoBinding.inflate(layoutInflater)
        dialogBinding.inputCodigo.setText(codigoInicial)
        dialogBinding.inputCodigo.isEnabled = codigoEditable

        // Restringir entrada a solo números (solo si es editable)
        if (codigoEditable) {
            dialogBinding.inputCodigo.filters = arrayOf(
                InputFilter { source, start, end, dest, dstart, dend ->
                    for (i in start until end) {
                        if (!Character.isDigit(source[i])) {
                            return@InputFilter ""
                        }
                    }
                    null
                }
            )
        } else {
            // Si no es editable (edición de producto existente), no aplicar filtro numérico
            dialogBinding.inputCodigo.filters = arrayOf()
        }

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

                // Validaciones
                if (nombre.isBlank() || precio == null || precio < 0.0 || inventario < 0) {
                    toast("Nombre, precio e inventario válidos son requeridos")
                    return@setPositiveButton
                }

                // NUEVA VALIDACIÓN: si es manual forzado (nuevo producto manual), el código no puede estar vacío
                if (esManualForzado && codigo.isBlank()) {
                    toast("El código no puede estar vacío")
                    return@setPositiveButton
                }

                // Para productos existentes, si dejan el código vacío, mantener el anterior
                val codigoFinal = if (codigo.isBlank() && producto != null) {
                    producto.codigoBarras
                } else if (codigo.isBlank() && producto == null && !esManualForzado) {
                    // Si es un producto nuevo pero no forzado (ej. desde escáner), generar automático
                    viewModel.generarCodigoManualSugerido()
                } else {
                    codigo
                }

                // Si se aplicó el filtro numérico, el código solo tiene dígitos, pero por si acaso
                if (esManualForzado && !codigoFinal.all { it.isDigit() }) {
                    toast("El código solo debe contener números")
                    return@setPositiveButton
                }

                val tipo = if (codigoFinal.startsWith("MANUAL_", ignoreCase = true)) Producto.TIPO_MANUAL else Producto.TIPO_CODIGO_BARRAS
                val base = producto ?: Producto(nombre = nombre, precio = precio)

                // Calcular esManualFinal
                val esManualFinal = if (esManualForzado) {
                    true
                } else {
                    producto?.esManual ?: (tipo == Producto.TIPO_MANUAL)
                }

                viewModel.guardar(
                    base.copy(
                        nombre = nombre,
                        codigoBarras = codigoFinal,
                        precio = precio,
                        inventario = inventario,
                        tipoProducto = producto?.tipoProducto ?: tipo,
                        esManual = esManualFinal
                    )
                )
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
