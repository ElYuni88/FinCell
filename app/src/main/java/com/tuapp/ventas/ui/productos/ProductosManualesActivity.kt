package com.tuapp.ventas.ui.productosmanuales

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ActivityProductosManualesBinding
import com.tuapp.ventas.ui.simple.VentaDirectaDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductosManualesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductosManualesBinding
    private val viewModel: ProductosManualesViewModel by viewModels {
        ProductosManualesViewModelFactory((application as VentasApplication).repository)
    }
    private lateinit var adapter: ProductosManualesAdapter

    // Obtener extras del Intent
    private val modo: ModoOperacion by lazy {
        intent.getSerializableExtra(EXTRA_MODO) as? ModoOperacion ?: ModoOperacion.SIMPLE
    }
    private val cuentaId: Long by lazy {
        intent.getLongExtra(EXTRA_CUENTA_ID, -1L)
    }

    // Scope para operaciones largas que no se cancelan al destruir la actividad
    private val job = SupervisorJob()
    private val ioScope = CoroutineScope(job + Dispatchers.IO)

    companion object {
        const val EXTRA_MODO = "modo"
        const val EXTRA_CUENTA_ID = "cuenta_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductosManualesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (modo == ModoOperacion.SIMPLE) {
            "Seleccionar producto manual"
        } else {
            "Agregar a cuenta"
        }

        configurarRecycler()
        configurarBusqueda()
        observarProductos()
    }

    private fun configurarRecycler() {
        adapter = ProductosManualesAdapter { producto ->
            mostrarDialogoCantidad(producto)
        }
        binding.recyclerProductos.layoutManager = LinearLayoutManager(this)
        binding.recyclerProductos.adapter = adapter
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

    private fun observarProductos() {
        viewModel.productosFiltrados.observe(this) { productos ->
            adapter.submitList(productos)
            binding.tvEmpty.visibility = if (productos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun mostrarDialogoCantidad(producto: Producto) {
        // Verificar si estamos en modo cuenta y la cuenta existe
        if (modo == ModoOperacion.CUENTA && cuentaId <= 0) {
            Toast.makeText(this, "No hay cuenta seleccionada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        VentaDirectaDialog().apply {
            this.producto = producto
            this.modo = this@ProductosManualesActivity.modo
            onConfirmar = { _, _, _, _, cantidad ->
                // Lanzar la corrutina en el scope propio (no se cancela al destruir actividad)
                ioScope.launch {
                    try {
                        val repo = (application as VentasApplication).repository
                        // Ejecutar la operación con NonCancellable para evitar que se cancele
                        withContext(NonCancellable) {
                            if (modo == ModoOperacion.SIMPLE) {
                                repo.registrarVentaDirecta(producto, cantidad)
                            } else {
                                repo.agregarProductoACuenta(cuentaId, producto, cantidad)
                            }
                        }
                        // Si llegamos aquí, la operación fue exitosa
                        // Mostrar el Toast en el hilo principal
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProductosManualesActivity,
                                if (modo == ModoOperacion.SIMPLE) "Venta registrada: ${producto.nombre} x$cantidad"
                                else "${producto.nombre} x$cantidad agregado a la cuenta",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Cerrar la actividad después de mostrar el mensaje
                            finish()
                        }
                    } catch (e: Exception) {
                        // Mostrar el error en el hilo principal
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProductosManualesActivity,
                                e.message ?: "Error al procesar",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            onCancelar = { /* No hacer nada */ }
        }.show(supportFragmentManager, "venta_directa")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar el scope para evitar fugas de memoria
        job.cancel()
    }
}