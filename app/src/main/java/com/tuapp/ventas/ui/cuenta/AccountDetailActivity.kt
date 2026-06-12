package com.tuapp.ventas.ui.cuenta

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.Cuenta
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.relaciones.DetalleConProducto
import com.tuapp.ventas.databinding.ActivityAccountDetailBinding
import com.tuapp.ventas.ui.cuenta.adapters.DetalleCuentaAdapter
import com.tuapp.ventas.ui.scanner.BarcodeScannerActivity
import com.tuapp.ventas.ui.simple.VentaDirectaDialog
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.SoundUtils

/** Pantalla de cuenta abierta editable o cuenta cerrada en modo solo lectura. */
class AccountDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountDetailBinding
    private val cuentaId by lazy { intent.getLongExtra(EXTRA_CUENTA_ID, -1L) }
    private val viewModel: AccountDetailViewModel by viewModels { AccountDetailViewModelFactory((application as VentasApplication).repository, cuentaId) }
    private var adapter = crearAdapter(soloLectura = false)

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val codigo = result.data?.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE).orEmpty()
        if (codigo.isNotBlank()) {
            SoundUtils.beep()
            SoundUtils.vibrar(this)
            viewModel.procesarEscaneoDirecto(codigo)
        }
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) abrirScanner() else toast("Permiso de cámara requerido") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurarRecycler()
        configurarClicks()
        observarDatos()
    }

    private fun configurarRecycler() = with(binding.recyclerDetalle) {
        layoutManager = LinearLayoutManager(this@AccountDetailActivity)
        itemAnimator = DefaultItemAnimator().apply { addDuration = 180; removeDuration = 180; changeDuration = 180 }
        adapter = this@AccountDetailActivity.adapter
    }

    private fun configurarClicks() = with(binding) {
        btnVolver.setOnClickListener { finish() }
        btnEscanear.setOnClickListener { solicitarCamara() }
        btnCerrarCuenta.setOnClickListener { mostrarDialogoCerrar() }
        btnTicket.setOnClickListener { generarTicket() }
    }

    private fun observarDatos() {
        viewModel.cuenta.observe(this) { cuentaConDetalles ->
            cuentaConDetalles ?: return@observe
            val abierta = cuentaConDetalles.cuenta.estado == Cuenta.ESTADO_ABIERTA
            binding.txtCliente.text = cuentaConDetalles.cliente.nombre
            binding.txtFechas.text = if (abierta) {
                "Apertura: ${DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaApertura)}"
            } else {
                "Apertura: ${DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaApertura)} · Cierre: ${DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaCierre ?: 0)}"
            }
            binding.txtTotal.text = "Total: ${DateUtils.moneda(cuentaConDetalles.cuenta.total)}"
            binding.btnEscanear.visibility = if (abierta) View.VISIBLE else View.GONE
            binding.btnCerrarCuenta.visibility = if (abierta) View.VISIBLE else View.GONE
            binding.btnTicket.visibility = if (abierta) View.GONE else View.VISIBLE
            if (adapterSoloLectura() != !abierta) {
                adapter = crearAdapter(soloLectura = !abierta)
                binding.recyclerDetalle.adapter = adapter
            }
            adapter.submitList(cuentaConDetalles.detalles)
        }
        viewModel.ventaFinal.observe(this) { venta ->
            binding.txtMetodoPago.visibility = if (venta == null) View.GONE else View.VISIBLE
            binding.txtMetodoPago.text = venta?.let { "Pago: ${it.metodoPago} · ${DateUtils.fechaHora(it.fechaCierre)}" }.orEmpty()
        }
        viewModel.mensaje.observe(this) { toast(it) }
        viewModel.codigoProductoNuevo.observe(this) { codigo -> codigo?.let { mostrarDialogoProductoNuevo(it) } }
    }

    private fun crearAdapter(soloLectura: Boolean) = DetalleCuentaAdapter(
        soloLectura = soloLectura,
        onIncrementar = { item -> viewModel.cambiarCantidad(item.detalle.id, item.detalle.cantidad + 1) },
        onDecrementar = { item -> viewModel.cambiarCantidad(item.detalle.id, item.detalle.cantidad - 1) },
        onEliminar = ::confirmarEliminacion
    )

    private fun adapterSoloLectura(): Boolean = binding.btnEscanear.visibility != View.VISIBLE

    private fun confirmarEliminacion(item: DetalleConProducto) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Eliminar ${item.producto.nombre} de la cuenta?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarDetalle(item.detalle.id) }
            .show()
    }

    private fun generarTicket() {
        val cuenta = viewModel.cuenta.value ?: return
        val detalle = buildString {
            appendLine("Cliente: ${cuenta.cliente.nombre}")
            appendLine("Total: ${DateUtils.moneda(cuenta.cuenta.total)}")
            appendLine()
            cuenta.detalles.forEach { item ->
                appendLine("${item.producto.nombre} x${item.detalle.cantidad}  ${DateUtils.moneda(item.detalle.subtotal)}")
            }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Ticket de cuenta #${cuenta.cuenta.id}")
            .setMessage(detalle)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun mostrarDialogoCerrar() {
        val total = viewModel.cuenta.value?.cuenta?.total ?: 0.0
        CerrarCuentaDialog().apply {
            this.total = total
            onCerrar = { metodo, observaciones -> viewModel.cerrarCuenta(metodo, observaciones) }
        }.show(supportFragmentManager, "cerrar_cuenta")
    }

    private fun mostrarDialogoProductoNuevo(codigo: String) {
        VentaDirectaDialog().apply {
            this.codigoNuevo = codigo
            modo = ModoOperacion.CUENTA
            onConfirmar = { productoExistente, codigoEscaneado, nombre, precio, cantidad ->
                if (nombre.isNullOrBlank() || precio == null) {
                    toast("Nombre y precio requeridos")
                } else {
                    val cantidadFinal = if (cantidad > 0) cantidad else 1
                    viewModel.crearProductoYAgregar(
                        codigo = codigoEscaneado ?: codigo,
                        nombre = nombre,
                        precio = precio,
                        cantidad = cantidadFinal
                    )
                }
            }
        }.show(supportFragmentManager, "nuevo_producto_cuenta")
    }

    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) abrirScanner() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun abrirScanner() = scanLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
    private fun toast(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()

    companion object { const val EXTRA_CUENTA_ID = "cuenta_id" }
}
