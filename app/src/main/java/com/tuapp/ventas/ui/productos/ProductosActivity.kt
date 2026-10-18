package com.tuapp.ventas.ui.productos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ActivityProductosBinding
import com.tuapp.ventas.databinding.DialogAgregarProductoBinding
import com.tuapp.ventas.ui.base.BaseActivity
import com.tuapp.ventas.ui.exportar.ExportarIPBActivity
import com.tuapp.ventas.ui.estadisticas.EstadisticasActivity
import com.tuapp.ventas.ui.main.MainActivity
import com.tuapp.ventas.ui.scanner.BarcodeScannerActivity
import com.tuapp.ventas.utils.TextUtils
import com.tuapp.ventas.utils.ValidadorCodigoProducto

/** Pantalla de gestión de productos con alta por escáner, alta manual, edición y eliminación. */
class ProductosActivity : BaseActivity() {
    private lateinit var binding: ActivityProductosBinding
    private val viewModel: ProductosViewModel by viewModels { ProductosViewModelFactory((application as VentasApplication).repository) }
    private val adapter = ProductoAdapter(::mostrarDialogoEditar, ::confirmarEliminar, ::confirmarActivar)
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
        setHasFixedSize(true)
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
        swMostrarInactivos.setOnCheckedChangeListener { _, checked ->
            viewModel.setMostrandoInactivos(checked)
        }
        inputBuscarProductos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = filtrarProductos(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observarDatos() {
        viewModel.productos.observe(this) { productos ->
            productosCompletos = productos.sortedBy { it.nombre.lowercase() }
            filtrarProductos(binding.inputBuscarProductos.text?.toString().orEmpty())
        }
        viewModel.mensaje.observe(this) { toast(it) }
    }

    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) abrirScanner()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun abrirScanner() = scanLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))

    private fun mostrarDialogoAgregar(codigoEscaneado: String?, esManual: Boolean = true) {
        // ✅ CORRECCIÓN: Si viene del escáner, esManual debe ser false
        val esManualReal = if (codigoEscaneado.isNullOrBlank()) esManual else false
        mostrarDialogoProducto(
            titulo = if (codigoEscaneado == null) "Agregar producto manual" else "Agregar producto escaneado",
            producto = null,
            codigoInicial = codigoEscaneado.orEmpty(),
            codigoEditable = codigoEscaneado == null,
            esManualForzado = esManualReal
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

        // ❌ Se elimina el InputFilter que solo permitía números.
        // Ahora se permiten letras, números y guion medio.

        dialogBinding.inputNombre.setText(producto?.nombre.orEmpty())
        dialogBinding.inputPrecio.setText(producto?.precio?.toString().orEmpty())

        val stockDisponible = if (producto != null) {
            (producto.inventario - producto.vendidos).coerceAtLeast(0)
        } else 0

        if (producto == null) {
            dialogBinding.tvStockActual.visibility = View.GONE
            dialogBinding.inputEntradaStock.visibility = View.GONE
            dialogBinding.inputInventario.visibility = View.VISIBLE
            dialogBinding.inputInventario.setText("0")
            dialogBinding.inputInventario.hint = "Cantidad en inventario"
        } else {
            dialogBinding.tvStockActual.visibility = View.VISIBLE
            dialogBinding.tvStockActual.text = "Stock actual: $stockDisponible"
            dialogBinding.inputEntradaStock.visibility = View.VISIBLE
            dialogBinding.inputEntradaStock.setText("")
            dialogBinding.inputEntradaStock.hint = "Entrada de stock (+)"
            dialogBinding.inputInventario.visibility = View.GONE
        }

        // ✅ Variable local para saber si el código es válido actualmente
        var codigoValido = false

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            val botonGuardar = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)

            // ✅ Configurar validación reactiva
            val repo = (application as VentasApplication).repository
            ValidadorCodigoProducto.configurar(
                binding = dialogBinding,
                repo = repo,
                scope = lifecycleScope,
                productoOriginalId = producto?.id,
                campoEditable = codigoEditable,
                onEstadoCambiado = { estado ->
                    codigoValido = estado is ValidadorCodigoProducto.Estado.Valido ||
                            estado is ValidadorCodigoProducto.Estado.SoloLetras
                    botonGuardar.isEnabled = codigoValido
                },
                onCodigoNormalizado = { nuevoCodigo ->
                    // Actualizar el campo visible sin disparar el watcher de nuevo
                    if (dialogBinding.inputCodigo.text?.toString() != nuevoCodigo) {
                        dialogBinding.inputCodigo.setText(nuevoCodigo)
                        dialogBinding.inputCodigo.setSelection(nuevoCodigo.length)
                    }
                }
            )

            // ✅ Click del botón Guardar
            botonGuardar.setOnClickListener {
                if (!codigoValido) {
                    toast("Corrige el código antes de guardar")
                    return@setOnClickListener
                }

                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val codigo = com.tuapp.ventas.utils.CodigoBarrasUtils
                    .normalizar(dialogBinding.inputCodigo.text?.toString().orEmpty())

                if (nombre.isBlank() || precio == null || precio < 0.0) {
                    toast("Nombre y precio válidos son requeridos")
                    return@setOnClickListener
                }
                if (esManualForzado && codigo.isBlank()) {
                    toast("El código no puede estar vacío")
                    return@setOnClickListener
                }

                val codigoFinal = if (codigo.isBlank() && producto != null) {
                    producto.codigoBarras
                } else if (codigo.isBlank() && producto == null && !esManualForzado) {
                    viewModel.generarCodigoManualSugerido()
                } else codigo

                val tipo = if (codigoFinal.startsWith("MANUAL_", ignoreCase = true)) Producto.TIPO_MANUAL else Producto.TIPO_CODIGO_BARRAS
                val base = producto ?: Producto(nombre = nombre, precio = precio)

                val nuevoInventario = if (producto != null) {
                    val entradaStr = dialogBinding.inputEntradaStock.text?.toString()?.trim()
                    val entrada = if (!entradaStr.isNullOrEmpty()) entradaStr.toIntOrNull() ?: 0 else 0
                    (producto.inventario + entrada).coerceAtLeast(0)
                } else {
                    dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 0
                }

                val esManualFinal = if (esManualForzado) true else producto?.esManual ?: (tipo == Producto.TIPO_MANUAL)

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
                dialog.dismiss()
            }
        }

        dialog.show()
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

    private fun confirmarActivar(producto: Producto) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Activar producto")
            .setMessage("¿Activar \"${producto.nombre}\"?\n\nVolverá a estar disponible para la venta.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Activar") { _, _ -> viewModel.activar(producto) }
            .show()
    }

    private fun filtrarProductos(query: String) {
        val filtrados = if (query.isBlank()) {
            productosCompletos
        } else {
            productosCompletos.filter { producto ->
                TextUtils.contiene(producto.nombre, query) ||
                        producto.codigoBarras.contains(query, ignoreCase = true)
            }
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