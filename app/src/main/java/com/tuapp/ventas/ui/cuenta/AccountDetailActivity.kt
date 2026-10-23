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
import android.content.ClipData
import android.net.Uri
import androidx.core.content.FileProvider
import com.tuapp.ventas.R
import com.tuapp.ventas.ui.ticket.TicketContent
import com.tuapp.ventas.ui.ticket.TicketData
import com.tuapp.ventas.ui.ticket.TicketPreviewDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.Cuenta
import com.tuapp.ventas.ui.productosmanuales.ProductosManualesActivity
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.model.relaciones.DetalleConProducto
import com.tuapp.ventas.databinding.ActivityAccountDetailBinding
import com.tuapp.ventas.ui.cuenta.adapters.DetalleCuentaAdapter
import com.tuapp.ventas.ui.scanner.BarcodeScannerActivity
import com.tuapp.ventas.databinding.DialogAgregarProductoBinding
import com.tuapp.ventas.ui.common.ProductoNoEncontradoDialogFragment
import com.tuapp.ventas.utils.CodigoBarrasUtils
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.SoundUtils
import com.tuapp.ventas.utils.ValidadorCodigoProducto
import kotlinx.coroutines.launch
import java.io.File

/** Pantalla de cuenta abierta editable o cuenta cerrada en modo solo lectura. */
class AccountDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountDetailBinding
    private val cuentaId by lazy { intent.getLongExtra(EXTRA_CUENTA_ID, -1L) }
    private val viewModel: AccountDetailViewModel by viewModels { AccountDetailViewModelFactory((application as VentasApplication).repository, cuentaId) }
    private var adapter = crearAdapter(soloLectura = false)
    private val repo by lazy { (application as VentasApplication).repository }

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val codigo = result.data?.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE).orEmpty()
        if (codigo.isNotBlank()) {
            SoundUtils.beep()
            SoundUtils.vibrar(this)
            viewModel.procesarEscaneoDirecto(codigo)
        }
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) abrirScanner() else toast(getString(R.string.msg_permiso_camara_requerido)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurarRecycler()
        configurarClicks()
        observarDatos()
    }

    private fun configurarRecycler() = with(binding.recyclerDetalle) {
        setHasFixedSize(true)
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
                getString(R.string.lbl_apertura_fecha, DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaApertura))
            } else {
                getString(R.string.lbl_apertura_cierre_fecha, DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaApertura), DateUtils.fechaHora(cuentaConDetalles.cuenta.fechaCierre ?: 0))
            }
            binding.txtTotal.text = getString(R.string.lbl_total, DateUtils.moneda(cuentaConDetalles.cuenta.total))
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
            binding.txtMetodoPago.text = venta?.let { getString(R.string.lbl_pago_metodo_fecha, it.metodoPago, DateUtils.fechaHora(it.fechaCierre)) }.orEmpty()
        }
        viewModel.mensaje.observe(this) { toast(it) }
        viewModel.codigoProductoNuevo.observe(this) { codigo -> codigo?.let { mostrarDialogoProductoNuevo(it) } }
        viewModel.productoInactivo.observe(this) { producto ->
            producto?.let { mostrarDialogoReactivarCuenta(it) }
        }
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
            .setTitle(getString(R.string.title_eliminar_producto))
            .setMessage(getString(R.string.msg_confirmar_eliminar_producto_cuenta, item.producto.nombre))
            .setNegativeButton(getString(R.string.cancelar), null)
            .setPositiveButton(getString(R.string.eliminar)) { _, _ -> viewModel.eliminarDetalle(item.detalle.id) }
            .show()
    }

    private fun generarTicket() {
        val cuenta = viewModel.cuenta.value ?: return
        val venta = viewModel.ventaFinal.value

        lifecycleScope.launch {
            val pv = repo.obtenerPuntoVentaActivo()
            val nombrePv = pv?.nombre?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name)
            val data = TicketContent.construirDatos(
                cuenta = cuenta,
                ventaFinal = venta,
                nombrePuntoVenta = nombrePv,
                nombreCliente = nombreCliente(cuenta),
                mesa = mesaCuenta(cuenta)
            )

            TicketPreviewDialog().apply {
                this.data = data
                this.onExportarCompartir = { d -> exportarYCompartir(d) }
                this.onImprimir = {
                    Toast.makeText(
                        this@AccountDetailActivity,
                        getString(R.string.msg_impresion_proximamente),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.show(supportFragmentManager, "ticket_preview")
        }
    }

    private fun exportarYCompartir(data: TicketData) {
        val contenido = TicketContent.textoPlano(data)
        val nombreArchivo = "ticket_cuenta_${data.cuentaId}_${System.currentTimeMillis()}.txt"

        lifecycleScope.launch {
            val resultado = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    guardarEnMediaStore(nombreArchivo, contenido)
                } else {
                    guardarEnAlmacenamientoLegacy(nombreArchivo, contenido)
                }
            }

            resultado.onSuccess { uri ->
                Toast.makeText(this@AccountDetailActivity, getString(R.string.msg_ticket_guardado_descargas), Toast.LENGTH_SHORT).show()
                compartirTicket(uri, nombreArchivo)
            }.onFailure { error ->
                Toast.makeText(
                    this@AccountDetailActivity,
                    getString(R.string.msg_error_guardar_ticket, error.message.orEmpty()),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun guardarEnMediaStore(nombre: String, contenido: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo")
        contentResolver.openOutputStream(uri).use { output ->
            output?.write(contenido.toByteArray()) ?: error("No se pudo escribir")
        }
        return uri
    }

    private fun guardarEnAlmacenamientoLegacy(nombre: String, contenido: String): Uri {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()
        val file = File(downloads, nombre)
        file.writeText(contenido)
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }

    private fun compartirTicket(uri: Uri, nombreArchivo: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.lbl_ticket_asunto, nombreArchivo))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.msg_ticket_venta_adjunto))
            clipData = ClipData.newRawUri("ticket", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.title_compartir_ticket)))
    }

    private fun nombreCliente(cuenta: com.tuapp.ventas.data.model.relaciones.CuentaConDetalles): String {
        val nombre = cuenta.cliente?.nombre ?: cuenta.cuenta.nombreClienteTemporal ?: getString(R.string.lbl_cliente_temporal)
        return if (cuenta.cuenta.esClienteTemporal) getString(R.string.lbl_cliente_no_registrado, nombre) else nombre
    }

    private fun mesaCuenta(cuenta: com.tuapp.ventas.data.model.relaciones.CuentaConDetalles): String {
        return cuenta.cliente?.mesa ?: cuenta.cuenta.mesaTemporal ?: getString(R.string.lbl_mesa_no_especificada)
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
        val codigoNormalizado = CodigoBarrasUtils.normalizar(codigo)
        if (codigoNormalizado.isBlank()) {
            toast(getString(R.string.msg_codigo_barras_vacio))
            return
        }

        val dialogBinding = DialogAgregarProductoBinding.inflate(layoutInflater)
        dialogBinding.inputCodigo.setText(codigoNormalizado)
        dialogBinding.inputCodigo.isEnabled = false
        dialogBinding.inputInventario.hint = getString(R.string.hint_cantidad_inventario)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_agregar_producto_escaneado))
            .setView(dialogBinding.root)
            .setNegativeButton(getString(R.string.cancelar), null)
            .setPositiveButton(getString(R.string.btn_guardar_y_agregar), null)
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
                    toast(getString(R.string.msg_corrige_codigo_antes_guardar))
                    return@setOnClickListener
                }
                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val cantidadInventario = dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 1
                if (nombre.isBlank() || precio == null || precio < 0.0 || cantidadInventario < 0) {
                    toast(getString(R.string.msg_campos_producto_requeridos))
                    return@setOnClickListener
                }
                botonGuardar.isEnabled = false
                viewModel.crearProductoYAgregar(
                    codigo = codigoNormalizado,
                    nombre = nombre,
                    precio = precio,
                    cantidad = 1,
                    inventario = cantidadInventario
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }


    private fun mostrarDialogoReactivarCuenta(producto: Producto) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_producto_inactivo))
            .setMessage(getString(R.string.msg_producto_inactivo_activar, producto.nombre))
            .setPositiveButton(getString(R.string.btn_activar)) { _, _ -> viewModel.activarYAgregar(producto) }
            .setNegativeButton(getString(R.string.btn_no)) { _, _ -> viewModel.consumirProductoInactivo() }
            .setCancelable(false)
            .show()
    }


    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) abrirScanner()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun abrirScanner() = scanLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
    private fun toast(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()

    companion object { const val EXTRA_CUENTA_ID = "cuenta_id" }
}