package com.tuapp.ventas.ui.venta

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.model.VentaItem
import com.tuapp.ventas.databinding.ActivityOperacionesBinding
import com.tuapp.ventas.databinding.DialogAgregarProductoBinding
import com.tuapp.ventas.ui.common.ProductoNoEncontradoDialogFragment
import com.tuapp.ventas.ui.productosmanuales.ProductosManualesActivity
import com.tuapp.ventas.ui.escaneo.EscaneoContinuoActivity
import com.tuapp.ventas.ui.simple.VentaDirectaDialog
import com.tuapp.ventas.utils.CodigoBarrasUtils
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager
import com.tuapp.ventas.utils.ValidadorCodigoProducto

/** Pantalla de venta directa con acumulación de múltiples productos. */
class OperacionesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOperacionesBinding
    private val viewModel: VentaMultipleViewModel by viewModels { VentaMultipleViewModelFactory((application as VentasApplication).repository) }
    private lateinit var adapter: VentaMultipleAdapter
    private lateinit var prefs: PreferencesManager
    private val repo by lazy { (application as VentasApplication).repository }

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val productos = result.data?.getSerializableExtra(EXTRA_PRODUCTOS_ACUMULADOS) as? ArrayList<VentaItem>
            if (!productos.isNullOrEmpty()) viewModel.agregarProductos(productos)
        }
    }

    private val manualLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.getLongExtra(ProductosManualesActivity.EXTRA_PRODUCTO_ID, -1L) ?: -1L
        if (result.resultCode == RESULT_OK && id > 0) viewModel.buscarProductoPorId(id)
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) abrirScanner() else Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferencesManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val productoInicial = intent.getSerializableExtra(EXTRA_PRODUCTO_INICIAL) as? Producto
        val cantidadInicial = intent.getIntExtra(EXTRA_CANTIDAD_INICIAL, 1)
        if (productoInicial != null) {
            viewModel.agregarProducto(productoInicial, cantidadInicial)
        }
        val productosIniciales = intent.getSerializableExtra(EXTRA_PRODUCTOS_ACUMULADOS) as? ArrayList<VentaItem>
        if (!productosIniciales.isNullOrEmpty()) viewModel.agregarProductos(productosIniciales)
        configurarRecycler()
        configurarClicks()
        observarDatos()
    }

    private fun configurarRecycler() {
        adapter = VentaMultipleAdapter { viewModel.eliminarProducto(it) }
        binding.recyclerVentaMultiple.setHasFixedSize(true)
        binding.recyclerVentaMultiple.layoutManager = LinearLayoutManager(this)
        binding.recyclerVentaMultiple.adapter = adapter
    }

    private fun configurarClicks() = with(binding) {
        btnEscanear.setOnClickListener { solicitarCamara() }
        btnAgregarManual.setOnClickListener { abrirProductosManuales() }
        btnFinalizarVenta.setOnClickListener { finalizarVenta() }
        // ✅ NUEVO: botón Cancelar operación
        btnCancelarOperacion.setOnClickListener { confirmarCancelarOperacion() }
    }

    private fun observarDatos() {
        viewModel.productosAcumulados.observe(this) { items ->
            adapter.submitList(items)
            binding.txtTotalAcumulado.text = DateUtils.moneda(items.sumOf { it.subtotal })
            binding.txtEmpty.text = if (items.isEmpty()) "Escanea o agrega productos para iniciar la venta" else ""
            binding.btnFinalizarVenta.isEnabled = items.isNotEmpty()
            // ✅ NUEVO: habilitar/deshabilitar botón cancelar según haya productos
            binding.btnCancelarOperacion.isEnabled = items.isNotEmpty()
            binding.btnCancelarOperacion.alpha = if (items.isNotEmpty()) 1f else 0.5f
        }
        viewModel.productoEscaneado.observe(this) { producto ->
            producto?.let { viewModel.consumirProductoEscaneado(); mostrarDialogoCantidad(it) }
        }
        viewModel.codigoNoEncontrado.observe(this) { codigo ->
            codigo?.let { viewModel.consumirCodigoNoEncontrado(); manejarProductoNoEncontrado(it) }
        }
        viewModel.productoCreado.observe(this) { producto ->
            producto?.let { viewModel.consumirProductoCreado(); mostrarDialogoCantidad(it) }
        }
        viewModel.mensaje.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
    }

    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) abrirScanner()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun abrirScanner() = scanLauncher.launch(Intent(this, EscaneoContinuoActivity::class.java).putExtra(EscaneoContinuoActivity.EXTRA_MODO_AGREGAR, true))

    private fun abrirProductosManuales() {
        manualLauncher.launch(Intent(this, ProductosManualesActivity::class.java).apply {
            putExtra(ProductosManualesActivity.EXTRA_MODO, ModoOperacion.SIMPLE)
            putExtra(ProductosManualesActivity.EXTRA_SELECCION, true)
        })
    }

    private fun mostrarDialogoCantidad(producto: Producto) {
        VentaDirectaDialog().apply {
            this.producto = producto
            modo = ModoOperacion.SIMPLE
            onConfirmar = { p, _, _, _, cantidad -> viewModel.agregarProducto(p ?: producto, cantidad) }
        }.show(supportFragmentManager, "venta_multiple_cantidad")
    }

    private fun manejarProductoNoEncontrado(codigo: String) {
        ProductoNoEncontradoDialogFragment.newInstance(codigo).apply {
            onDarEntrada = { codigoEscaneado -> mostrarDialogoAltaProducto(codigoEscaneado) }
        }.show(supportFragmentManager, "producto_no_encontrado")
    }

    private fun mostrarDialogoAltaProducto(codigo: String) {
        val codigoNormalizado = CodigoBarrasUtils.normalizar(codigo)
        if (codigoNormalizado.isBlank()) {
            Toast.makeText(this, "El código de barras no puede estar vacío", Toast.LENGTH_LONG).show()
            return
        }

        val dialogBinding = DialogAgregarProductoBinding.inflate(layoutInflater)
        dialogBinding.inputCodigo.setText(codigoNormalizado)
        dialogBinding.inputCodigo.isEnabled = false
        dialogBinding.inputInventario.hint = "Cantidad en inventario"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar producto escaneado")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            val botonGuardar = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            var codigoValido = false

            ValidadorCodigoProducto.configurar(
                binding = dialogBinding,
                repo = repo,
                scope = lifecycleScope,
                campoEditable = false,
                onEstadoCambiado = { estado ->
                    codigoValido = estado is ValidadorCodigoProducto.Estado.Valido ||
                            estado is ValidadorCodigoProducto.Estado.SoloLetras
                    botonGuardar.isEnabled = codigoValido
                }
            )

            botonGuardar.setOnClickListener {
                if (!codigoValido) {
                    Toast.makeText(this, "Corrige el código antes de guardar", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val inventario = dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 1
                if (nombre.isBlank() || precio == null || precio < 0.0 || inventario < 0) {
                    Toast.makeText(this, "Nombre, precio e inventario válidos son requeridos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                botonGuardar.isEnabled = false
                viewModel.crearProductoEscaneado(codigoNormalizado, nombre, precio, inventario)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun finalizarVenta() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Finalizar venta")
            .setMessage("¿Registrar todos los productos acumulados?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Registrar") { _, _ ->
                viewModel.registrarVenta { mensaje -> Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show(); if (mensaje.contains("registrada")) finish() }
            }.show()
    }

    /**
     * ✅ NUEVO: Confirmar y ejecutar cancelación de la operación.
     * Limpia todos los productos acumulados y cierra la pantalla.
     */
    private fun confirmarCancelarOperacion() {
        val items = viewModel.productosAcumulados.value.orEmpty()
        if (items.isEmpty()) {
            finish()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Cancelar operación")
            .setMessage(
                "¿Cancelar la operación actual?\n\n" +
                        "Se descartarán ${items.size} producto(s) acumulado(s).\n\n" +
                        "Esta acción no se puede deshacer."
            )
            .setNegativeButton("No", null)
            .setPositiveButton("Sí, cancelar") { _, _ ->
                viewModel.limpiarCarrito()
                Toast.makeText(this, "Operación cancelada", Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        // ✅ Si el usuario presiona "atrás" y hay productos, preguntar antes de salir
        val items = viewModel.productosAcumulados.value.orEmpty()
        if (items.isNotEmpty()) {
            confirmarCancelarOperacion()
        } else {
            finish()
        }
        return true
    }

    companion object {
        private const val TAG = "OperacionesActivity"
        const val EXTRA_PRODUCTO_INICIAL = "producto_inicial"
        const val EXTRA_CANTIDAD_INICIAL = "cantidad_inicial"
        const val EXTRA_PRODUCTOS_ACUMULADOS = EscaneoContinuoActivity.EXTRA_PRODUCTOS_ACUMULADOS
    }
}