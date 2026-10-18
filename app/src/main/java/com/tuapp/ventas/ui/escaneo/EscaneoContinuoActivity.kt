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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import androidx.appcompat.app.AlertDialog
import com.tuapp.ventas.databinding.ActivityEscaneoContinuoBinding
import com.tuapp.ventas.databinding.DialogAgregarProductoBinding
import com.tuapp.ventas.ui.common.ProductoNoEncontradoDialogFragment
import com.tuapp.ventas.ui.simple.VentaDirectaDialog
import com.tuapp.ventas.ui.venta.OperacionesActivity
import com.tuapp.ventas.utils.CodigoBarrasUtils
import com.tuapp.ventas.utils.PreferencesManager
import com.tuapp.ventas.utils.SoundUtils
import com.tuapp.ventas.utils.ValidadorCodigoProducto
import kotlinx.coroutines.launch

class EscaneoContinuoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEscaneoContinuoBinding
    private val viewModel: EscaneoContinuoViewModel by viewModels { EscaneoContinuoViewModelFactory((application as VentasApplication).repository) }
    private val repo by lazy { (application as VentasApplication).repository }
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    // ✅ MEJORA: Limitar formatos de ML Kit a códigos de barras de productos.
    // Esto evita leer QRs, PDF417, Aztec, etc. que causan falsas lecturas.
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_CODABAR
            )
            .build()
    )

    private lateinit var prefs: PreferencesManager
    private var pausadoPorDialogo = false
    private var ultimoCodigo: String? = null
    private var ultimoCodigoMs: Long = 0L
    private var dialogoAltaProducto: AlertDialog? = null
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
            producto?.let {
                viewModel.consumirProductoEscaneado()
                manejarProductoEncontrado(it)
            }
        }
        viewModel.codigoNoEncontrado.observe(this) { codigo ->
            codigo?.let { viewModel.consumirCodigoNoEncontrado(); manejarProductoNoEncontrado(it) }
        }
        viewModel.productoCreado.observe(this) { producto ->
            producto?.let {
                viewModel.consumirProductoCreado()
                dialogoAltaProducto?.dismiss()
                dialogoAltaProducto = null
                mostrarDialogoCantidad(it)
            }
        }
        viewModel.mensaje.observe(this) {
            dialogoAltaProducto?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
    }

    private fun manejarProductoEncontrado(producto: Producto) {
        if (producto.inactivo) {
            mostrarDialogoReactivar(producto)
        } else {
            mostrarDialogoCantidad(producto)
        }
    }

    private fun mostrarDialogoReactivar(producto: Producto) {
        pausadoPorDialogo = true
        MaterialAlertDialogBuilder(this)
            .setTitle("Producto inactivo")
            .setMessage(
                "\"${producto.nombre}\" está inactivo (lo desactivó el admin).\n\n" +
                        "¿Quieres activarlo y continuar con la venta?"
            )
            .setPositiveButton("Activar") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repo.activarProducto(producto.id) }
                        .onSuccess {
                            mostrarDialogoCantidad(producto.copy(inactivo = false))
                        }
                        .onFailure {
                            Toast.makeText(
                                this@EscaneoContinuoActivity,
                                "No se pudo activar: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            reanudarEscaneo()
                        }
                }
            }
            .setNegativeButton("No") { _, _ -> reanudarEscaneo() }
            .setCancelable(false)
            .show()
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
            // ✅ NUEVO: si cancela, reanudar el escaneo
            onCancelar = { reanudarEscaneo() }
        }.show(supportFragmentManager, "producto_no_encontrado")
    }

    private fun mostrarDialogoAltaProducto(codigo: String) {
        val codigoNormalizado = CodigoBarrasUtils.normalizar(codigo)
        if (codigoNormalizado.isBlank()) {
            Toast.makeText(this, "El código de barras no puede estar vacío", Toast.LENGTH_LONG).show()
            reanudarEscaneo()
            return
        }

        val dialogBinding = DialogAgregarProductoBinding.inflate(layoutInflater)
        dialogBinding.inputCodigo.setText(codigoNormalizado)
        dialogBinding.inputCodigo.isEnabled = false
        dialogBinding.inputInventario.hint = "Cantidad en inventario"

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar producto escaneado")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar") { _, _ -> reanudarEscaneo() }
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            val botonGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
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
                    Toast.makeText(
                        this@EscaneoContinuoActivity,
                        "Corrige el código antes de guardar",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val inventario = dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 1
                if (nombre.isBlank() || precio == null || precio < 0.0 || inventario < 0) {
                    Toast.makeText(
                        this@EscaneoContinuoActivity,
                        "Nombre, precio e inventario válidos son requeridos",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                botonGuardar.isEnabled = false
                viewModel.crearProductoEscaneado(codigoNormalizado, nombre, precio, inventario)
            }
        }
        dialog.setOnDismissListener { if (dialogoAltaProducto === dialog) dialogoAltaProducto = null }
        dialogoAltaProducto = dialog
        dialog.show()
    }

    /**
     * ✅ Reanuda el escaneo tras cerrar cualquier diálogo.
     * También resetea el último código para permitir leer el mismo código de nuevo.
     */
    private fun reanudarEscaneo() {
        pausadoPorDialogo = false
        // ✅ Resetear el último código para que el mismo se pueda escanear de nuevo inmediatamente
        ultimoCodigo = null
        ultimoCodigoMs = 0L
    }

    /**
     * Finaliza el escaneo.
     * - Sin productos → cierra la activity y vuelve a MainActivity.
     * - Con productos → va a OperacionesActivity (o devuelve los productos al llamador).
     */
    private fun finalizarEscaneo() {
        val productos = viewModel.obtenerProductosAcumulados()

        if (productos.isEmpty()) {
            // ✅ Sin productos: cerrar la pantalla y volver al origen
            finish()
            return
        }

        val data = Intent().putExtra(EXTRA_PRODUCTOS_ACUMULADOS, productos)
        if (modoAgregar) {
            setResult(RESULT_OK, data)
            finish()
        } else {
            startActivity(Intent(this, OperacionesActivity::class.java).putExtra(OperacionesActivity.EXTRA_PRODUCTOS_ACUMULADOS, productos))
            finish()
        }
    }

    private fun iniciarCamara() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { proxy -> analizarImagen(proxy) }
                }
            runCatching {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            }.onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarImagen(image: ImageProxy) {
        val media = image.image ?: run { image.close(); return }
        if (pausadoPorDialogo) { image.close(); return }
        scanner.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes ->
                // ✅ Tomar el primer código con longitud razonable
                val codigo = codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue.orEmpty()
                if (codigo.isBlank()) return@addOnSuccessListener

                // ✅ Filtro mínimo: códigos de barras de productos suelen tener 6-14 caracteres
                // Esto ayuda a descartar lecturas erráticas
                if (codigo.length < 4) return@addOnSuccessListener

                val ahora = System.currentTimeMillis()
                if (codigo != ultimoCodigo || ahora - ultimoCodigoMs > 1500L) {
                    ultimoCodigo = codigo
                    ultimoCodigoMs = ahora
                    pausadoPorDialogo = true
                    if (prefs.sonidoEscaneo) SoundUtils.beep()
                    if (prefs.vibrarEscaneo) SoundUtils.vibrar(this)
                    viewModel.procesarCodigo(codigo)
                }
            }.addOnCompleteListener { image.close() }
    }

    override fun onDestroy() {
        scanner.close()
        cameraProvider?.unbindAll()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODO_AGREGAR = "extra_modo_agregar"
        const val EXTRA_PRODUCTOS_ACUMULADOS = "extra_productos_acumulados"
    }
}