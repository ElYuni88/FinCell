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
            dialogBinding.inputCodigo.filters = arrayOf()
        }

        dialogBinding.inputNombre.setText(producto?.nombre.orEmpty())
        dialogBinding.inputPrecio.setText(producto?.precio?.toString().orEmpty())

        // --- Configurar visibilidad según nuevo/edición ---
        val stockDisponible = if (producto != null) {
            (producto.inventario - producto.vendidos).coerceAtLeast(0)
        } else {
            0
        }

        if (producto == null) {
            // ✅ NUEVO PRODUCTO: mostrar inventario, ocultar entrada y stock actual
            dialogBinding.tvStockActual.visibility = View.GONE
            dialogBinding.inputEntradaStock.visibility = View.GONE
            dialogBinding.inputInventario.visibility = View.VISIBLE
            dialogBinding.inputInventario.setText("0")
            dialogBinding.inputInventario.hint = "Cantidad en inventario"
        } else {
            // ✅ EDICIÓN: mostrar stock actual y entrada, ocultar inventario
            dialogBinding.tvStockActual.visibility = View.VISIBLE
            dialogBinding.tvStockActual.text = "Stock actual: $stockDisponible"
            dialogBinding.inputEntradaStock.visibility = View.VISIBLE
            dialogBinding.inputEntradaStock.setText("")
            dialogBinding.inputEntradaStock.hint = "Entrada de stock (+)"
            dialogBinding.inputInventario.visibility = View.GONE
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val codigo = dialogBinding.inputCodigo.text?.toString()?.trim().orEmpty()

                // Validaciones
                if (nombre.isBlank() || precio == null || precio < 0.0) {
                    toast("Nombre y precio válidos son requeridos")
                    return@setPositiveButton
                }

                if (esManualForzado && codigo.isBlank()) {
                    toast("El código no puede estar vacío")
                    return@setPositiveButton
                }

                val codigoFinal = if (codigo.isBlank() && producto != null) {
                    producto.codigoBarras
                } else if (codigo.isBlank() && producto == null && !esManualForzado) {
                    viewModel.generarCodigoManualSugerido()
                } else {
                    codigo
                }

                if (esManualForzado && !codigoFinal.all { it.isDigit() }) {
                    toast("El código solo debe contener números")
                    return@setPositiveButton
                }

                val tipo = if (codigoFinal.startsWith("MANUAL_", ignoreCase = true)) Producto.TIPO_MANUAL else Producto.TIPO_CODIGO_BARRAS
                val base = producto ?: Producto(nombre = nombre, precio = precio)

                // --- Calcular el nuevo inventario según el caso ---
                val nuevoInventario = if (producto != null) {
                    // EDICIÓN: sumar entrada al inventario actual
                    val entradaStr = dialogBinding.inputEntradaStock.text?.toString()?.trim()
                    val entrada = if (!entradaStr.isNullOrEmpty()) {
                        entradaStr.toIntOrNull() ?: 0
                    } else {
                        0
                    }
                    (producto.inventario + entrada).coerceAtLeast(0)
                } else {
                    // NUEVO PRODUCTO: usar el valor del campo inventario
                    dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 0
                }

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
                        inventario = nuevoInventario,
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
