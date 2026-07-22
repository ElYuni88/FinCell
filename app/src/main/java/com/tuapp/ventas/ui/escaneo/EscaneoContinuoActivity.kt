package com.tuapp.ventas.ui.escaneo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.model.VentaItem
import com.tuapp.ventas.databinding.ActivityEscaneoContinuoBinding
import com.tuapp.ventas.databinding.DialogAgregarProductoBinding
import com.tuapp.ventas.ui.common.ProductoNoEncontradoDialogFragment
import com.tuapp.ventas.ui.simple.VentaDirectaDialog
import com.tuapp.ventas.ui.venta.OperacionesActivity
import com.tuapp.ventas.utils.PreferencesManager
import com.tuapp.ventas.utils.SoundUtils

class EscaneoContinuoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEscaneoContinuoBinding
    private val viewModel: EscaneoContinuoViewModel by viewModels { EscaneoContinuoViewModelFactory((application as VentasApplication).repository) }
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val scanner = BarcodeScanning.getClient()
    private lateinit var prefs: PreferencesManager
    private var pausadoPorDialogo = false
    private var ultimoCodigo: String? = null
    private var ultimoCodigoMs: Long = 0L
    private val modoAgregar: Boolean get() = intent.getBooleanExtra(EXTRA_MODO_AGREGAR, false)

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) iniciarCamara() else { Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_LONG).show(); finish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEscaneoContinuoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferencesManager(this)
        binding.btnTerminar.setOnClickListener { finalizarEscaneo() }
        observarDatos()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) iniciarCamara()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun observarDatos() {
        viewModel.cantidadProductos.observe(this) { binding.tvContador.text = "Productos: $it" }
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

    private fun mostrarDialogoCantidad(producto: Producto) {
        pausadoPorDialogo = true
        val productoLimitado = viewModel.productoConStockDisponible(producto)
        VentaDirectaDialog().apply {
            this.producto = productoLimitado
            modo = ModoOperacion.SIMPLE
            mostrarBotonSeguir = false
            onConfirmar = { p, _, _, _, cantidad ->
                viewModel.agregarProducto(producto, cantidad)
                reanudarEscaneo()
            }
            onCancelar = { reanudarEscaneo() }
        }.show(supportFragmentManager, "cantidad_escaneo")
    }

    private fun manejarProductoNoEncontrado(codigo: String) {
        pausadoPorDialogo = true
        ProductoNoEncontradoDialogFragment.newInstance(codigo).apply {
            onDarEntrada = { codigoEscaneado -> mostrarDialogoAltaProducto(codigoEscaneado) }
        }.show(supportFragmentManager, "producto_no_encontrado")
    }

    private fun mostrarDialogoAltaProducto(codigo: String) {
        val dialogBinding = DialogAgregarProductoBinding.inflate(layoutInflater)
        dialogBinding.inputCodigo.setText(codigo)
        dialogBinding.inputCodigo.isEnabled = false
        dialogBinding.inputInventario.hint = "Cantidad en inventario"
        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar producto escaneado")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar") { _, _ -> reanudarEscaneo() }
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val inventario = dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 1
                if (nombre.isBlank() || precio == null || precio < 0.0 || inventario < 0) {
                    Toast.makeText(this, "Nombre, precio e inventario válidos son requeridos", Toast.LENGTH_SHORT).show(); reanudarEscaneo()
                } else viewModel.crearProductoEscaneado(codigo, nombre, precio, inventario)
            }.show()
    }

    private fun reanudarEscaneo() { pausadoPorDialogo = false }

    private fun finalizarEscaneo() {
        val productos = viewModel.obtenerProductosAcumulados()
        if (productos.isEmpty()) { Toast.makeText(this, "No se escaneó ningún producto", Toast.LENGTH_SHORT).show(); return }
        val data = Intent().putExtra(EXTRA_PRODUCTOS_ACUMULADOS, productos)
        if (modoAgregar) { setResult(RESULT_OK, data); finish() }
        else { startActivity(Intent(this, OperacionesActivity::class.java).putExtra(OperacionesActivity.EXTRA_PRODUCTOS_ACUMULADOS, productos)); finish() }
    }

    private fun iniciarCamara() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
            imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this)) { proxy -> analizarImagen(proxy) }
            }
            runCatching { cameraProvider?.unbindAll(); cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis) }
                .onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarImagen(image: ImageProxy) {
        val media = image.image ?: run { image.close(); return }
        if (pausadoPorDialogo) { image.close(); return }
        scanner.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes ->
                val codigo = codes.firstOrNull()?.rawValue.orEmpty()
                val ahora = System.currentTimeMillis()
                if (codigo.isNotBlank() && (codigo != ultimoCodigo || ahora - ultimoCodigoMs > 1500L)) {
                    ultimoCodigo = codigo; ultimoCodigoMs = ahora; pausadoPorDialogo = true
                    if (prefs.sonidoEscaneo) SoundUtils.beep()
                    if (prefs.vibrarEscaneo) SoundUtils.vibrar(this)
                    viewModel.procesarCodigo(codigo)
                }
            }.addOnCompleteListener { image.close() }
    }

    override fun onDestroy() { scanner.close(); cameraProvider?.unbindAll(); super.onDestroy() }

    companion object {
        const val EXTRA_MODO_AGREGAR = "extra_modo_agregar"
        const val EXTRA_PRODUCTOS_ACUMULADOS = "extra_productos_acumulados"
    }
}
