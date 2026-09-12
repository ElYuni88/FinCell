package com.tuapp.ventas.ui.productosmanuales

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ActivityProductosManualesBinding
import com.tuapp.ventas.ui.simple.VentaDirectaDialog
import com.tuapp.ventas.utils.DateUtils

class ProductosManualesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductosManualesBinding
    private val viewModel: ProductosManualesViewModel by viewModels {
        ProductosManualesViewModelFactory((application as VentasApplication).repository)
    }
    private lateinit var productosAdapter: ProductosManualesAdapter
    private lateinit var carritoAdapter: VentaTemporalAdapter

    // Extras del Intent
    private val modo: ModoOperacion by lazy {
        intent.getSerializableExtra(EXTRA_MODO) as? ModoOperacion ?: ModoOperacion.SIMPLE
    }
    private val cuentaId: Long by lazy {
        intent.getLongExtra(EXTRA_CUENTA_ID, -1L)
    }
    private val modoSeleccion: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_SELECCION, false)
    }

    companion object {
        const val EXTRA_MODO = "modo"
        const val EXTRA_CUENTA_ID = "cuenta_id"
        const val EXTRA_SELECCION = "seleccion"
        const val EXTRA_PRODUCTO_ID = "producto_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductosManualesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (modoSeleccion) {
            "Seleccionar producto"
        } else {
            "Venta manual"
        }

        configurarRecyclers()
        configurarBusqueda()
        configurarBotones()
        observarDatos()
    }

    private fun configurarRecyclers() {
        // Adapter de productos (catálogo)
        productosAdapter = ProductosManualesAdapter { producto ->
            if (modoSeleccion) {
                devolverProductoSeleccionado(producto)
            } else {
                mostrarDialogoCantidad(producto)
            }
        }
        binding.recyclerProductos.layoutManager = LinearLayoutManager(this)
        binding.recyclerProductos.adapter = productosAdapter
        binding.recyclerProductos.setHasFixedSize(true)

        // Adapter del carrito temporal
        carritoAdapter = VentaTemporalAdapter { item ->
            viewModel.eliminarDelCarrito(item)
        }
        binding.recyclerCarrito.layoutManager = LinearLayoutManager(this)
        binding.recyclerCarrito.adapter = carritoAdapter
        binding.recyclerCarrito.setHasFixedSize(true)
    }

    private fun configurarBusqueda() {
        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun configurarBotones() {
        binding.btnRegistrarVenta.setOnClickListener { confirmarVenta() }
        binding.btnCancelarVenta.setOnClickListener { confirmarCancelar() }
    }

    private fun observarDatos() {
        // Productos filtrados
        viewModel.productosFiltrados.observe(this) { productos ->
            productosAdapter.submitList(productos)
            binding.tvEmpty.visibility = if (productos.isEmpty()) View.VISIBLE else View.GONE
        }

        // Carrito temporal
        viewModel.productosAcumulados.observe(this) { items ->
            carritoAdapter.submitList(items)

            if (items.isEmpty()) {
                binding.layoutCarrito.visibility = View.GONE
            } else {
                binding.layoutCarrito.visibility = View.VISIBLE
                binding.txtTotalCarrito.text = "Total: ${DateUtils.moneda(items.sumOf { it.subtotal })}"
            }
        }

        // Mensajes
        viewModel.mensaje.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        // Venta registrada → cerrar
        viewModel.ventaRegistrada.observe(this) { registrada ->
            if (registrada) {
                viewModel.consumirVentaRegistrada()
                finish()
            }
        }
    }

    private fun devolverProductoSeleccionado(producto: Producto) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_PRODUCTO_ID, producto.id))
        finish()
    }

    private fun mostrarDialogoCantidad(producto: Producto) {
        // Validar modo CUENTA
        if (modo == ModoOperacion.CUENTA && cuentaId <= 0) {
            Toast.makeText(this, "No hay cuenta seleccionada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        VentaDirectaDialog().apply {
            this.producto = producto
            this.modo = this@ProductosManualesActivity.modo
            onConfirmar = { _, _, _, _, cantidad ->
                // ✅ AGREGAR AL CARRITO en lugar de registrar directamente
                viewModel.agregarAlCarrito(producto, cantidad)
            }
            onCancelar = { /* No hacer nada */ }
        }.show(supportFragmentManager, "venta_directa")
    }

    private fun confirmarVenta() {
        val items = viewModel.productosAcumulados.value.orEmpty()
        if (items.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un producto", Toast.LENGTH_SHORT).show()
            return
        }

        val total = items.sumOf { it.subtotal }
        val modoTexto = if (modo == ModoOperacion.SIMPLE) "venta directa" else "la cuenta"

        MaterialAlertDialogBuilder(this)
            .setTitle("Registrar venta")
            .setMessage("¿Registrar ${items.size} productos por ${DateUtils.moneda(total)} en $modoTexto?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Registrar") { _, _ ->
                viewModel.registrarVenta(
                    modo = if (modo == ModoOperacion.SIMPLE) "SIMPLE" else "CUENTA",
                    cuentaId = cuentaId
                )
            }
            .show()
    }

    private fun confirmarCancelar() {
        if (viewModel.productosAcumulados.value.isNullOrEmpty()) {
            finish()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Cancelar venta")
            .setMessage("¿Descartar todos los productos agregados?")
            .setNegativeButton("No", null)
            .setPositiveButton("Sí, descartar") { _, _ ->
                viewModel.limpiarCarrito()
                finish()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}