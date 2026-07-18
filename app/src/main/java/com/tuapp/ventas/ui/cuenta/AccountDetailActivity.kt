package com.tuapp.ventas.ui.cuenta

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.provider.MediaStore
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
import com.tuapp.ventas.ui.productosmanuales.ProductosManualesActivity
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.relaciones.DetalleConProducto
import com.tuapp.ventas.databinding.ActivityAccountDetailBinding
import com.tuapp.ventas.ui.cuenta.adapters.DetalleCuentaAdapter
import com.tuapp.ventas.ui.scanner.BarcodeScannerActivity
import com.tuapp.ventas.databinding.DialogAgregarProductoBinding
import com.tuapp.ventas.ui.common.ProductoNoEncontradoDialogFragment
import com.tuapp.ventas.ui.simple.AgregarProductoManualDialog
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.SoundUtils
import java.io.File

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
        btnAgregarManual.setOnClickListener { mostrarDialogoAgregarManual() }
        btnCerrarCuenta.setOnClickListener { mostrarDialogoCerrar() }
        btnTicket.setOnClickListener { generarTicket() }
    }

    private fun observarDatos() {
        viewModel.cuenta.observe(this) { cuentaConDetalles ->
            cuentaConDetalles ?: return@observe
            val abierta = cuentaConDetalles.cuenta.estado == Cuenta.ESTADO_ABIERTA
            binding.txtCliente.text = nombreCliente(cuentaConDetalles)
            binding.txtFechas.text = if (abierta) {
                "Apertura: ${DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaApertura)}"
            } else {
                "Apertura: ${DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaApertura)} · Cierre: ${DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaCierre ?: 0)}"
            }
            binding.txtTotal.text = "Total: ${DateUtils.moneda(cuentaConDetalles.cuenta.total)}"
            binding.btnEscanear.visibility = if (abierta) View.VISIBLE else View.GONE
            binding.btnAgregarManual.visibility = if (abierta) View.VISIBLE else View.GONE
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
        MaterialAlertDialogBuilder(this)
            .setTitle("Exportar Ticket")
            .setMessage("¿Desea exportar el ticket con la información de la cuenta?")
            .setPositiveButton("Exportar") { _, _ -> exportarTicketArchivo() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportarTicketArchivo() {
        val cuenta = viewModel.cuenta.value ?: return
        val venta = viewModel.ventaFinal.value
        val contenido = buildString {
            appendLine("=== TICKET DE CUENTA ===")
            appendLine("Cuenta: #${cuenta.cuenta.id}")
            appendLine("Cliente: ${nombreCliente(cuenta)}")
            appendLine("Mesa: ${mesaCuenta(cuenta)}")
            appendLine("Fecha apertura: ${DateUtils.fechaHora(cuenta.cuenta.fechaApertura)}")
            appendLine("Fecha cierre: ${DateUtils.fechaHora(cuenta.cuenta.fechaCierre ?: System.currentTimeMillis())}")
            appendLine("Método de pago: ${venta?.metodoPago ?: "No especificado"}")
            appendLine()
            appendLine("Productos:")
            cuenta.detalles.forEach { item ->
                appendLine("- ${item.producto.nombre} x${item.detalle.cantidad} = ${DateUtils.moneda(item.detalle.subtotal)}")
            }
            appendLine()
            appendLine("TOTAL: ${DateUtils.moneda(cuenta.cuenta.total)}")
        }

        val nombreArchivo = "ticket_cuenta_${cuenta.cuenta.id}_${System.currentTimeMillis()}.txt"
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("No se pudo crear el archivo en Descargas")
                contentResolver.openOutputStream(uri).use { output ->
                    output?.write(contenido.toByteArray()) ?: error("No se pudo escribir el ticket")
                }
                nombreArchivo
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                val file = File(downloads, nombreArchivo)
                file.writeText(contenido)
                file.name
            }
        }.onSuccess { archivo ->
            Toast.makeText(this, "Ticket guardado en Descargas: $archivo", Toast.LENGTH_LONG).show()
        }.onFailure { error ->
            Toast.makeText(this, "No se pudo guardar el ticket: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun nombreCliente(cuenta: com.tuapp.ventas.data.model.relaciones.CuentaConDetalles): String {
        val nombre = cuenta.cliente?.nombre ?: cuenta.cuenta.nombreClienteTemporal ?: "Cliente temporal"
        return if (cuenta.cuenta.esClienteTemporal) "$nombre (No registrado)" else nombre
    }

    private fun mesaCuenta(cuenta: com.tuapp.ventas.data.model.relaciones.CuentaConDetalles): String {
        return cuenta.cliente?.mesa ?: cuenta.cuenta.mesaTemporal ?: "No especificada"
    }

    private fun mostrarDialogoCerrar() {
        val total = viewModel.cuenta.value?.cuenta?.total ?: 0.0
        CerrarCuentaDialog().apply {
            this.total = total
            onCerrar = { metodo, observaciones -> viewModel.cerrarCuenta(metodo, observaciones) }
        }.show(supportFragmentManager, "cerrar_cuenta")
    }

    private fun mostrarDialogoAgregarManual() {
        val intent = Intent(this, ProductosManualesActivity::class.java).apply {
            putExtra(ProductosManualesActivity.EXTRA_MODO, ModoOperacion.CUENTA)
            putExtra(ProductosManualesActivity.EXTRA_CUENTA_ID, cuentaId)
        }
        startActivity(intent)
    }

    private fun mostrarDialogoProductoNuevo(codigo: String) {
        ProductoNoEncontradoDialogFragment.newInstance(codigo).apply {
            onDarEntrada = { codigoEscaneado -> mostrarDialogoAltaProductoEscaneado(codigoEscaneado) }
        }.show(supportFragmentManager, "producto_no_encontrado_cuenta")
    }

    private fun mostrarDialogoAltaProductoEscaneado(codigo: String) {
        val dialogBinding = DialogAgregarProductoBinding.inflate(layoutInflater)
        dialogBinding.inputCodigo.setText(codigo)
        dialogBinding.inputCodigo.isEnabled = false
        dialogBinding.inputInventario.hint = "Cantidad en inventario"
        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar producto escaneado")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar y agregar") { _, _ ->
                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val cantidadInventario = dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 1
                if (nombre.isBlank() || precio == null || precio < 0.0 || cantidadInventario < 0) {
                    toast("Nombre, precio y cantidad en inventario válidos son requeridos")
                } else {
                    viewModel.crearProductoYAgregar(codigo = codigo, nombre = nombre, precio = precio, cantidad = 1, inventario = cantidadInventario)
                }
            }
            .show()
    }

    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) abrirScanner() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun abrirScanner() = scanLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
    private fun toast(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()

    companion object { const val EXTRA_CUENTA_ID = "cuenta_id" }
}
