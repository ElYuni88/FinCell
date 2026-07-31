package com.tuapp.ventas.ui.scanner

import android.content.Intent
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tuapp.ventas.R
import com.tuapp.ventas.databinding.ActivityScannerBinding

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private var flashEnabled = false
    private var camera: Camera? = null
    private var procesado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCancelar.setOnClickListener {
            finish()
        }

        // Configurar el botón de linterna
        binding.fabFlash.setOnClickListener {
            flashEnabled = !flashEnabled
            camera?.cameraControl?.enableTorch(flashEnabled)
            binding.fabFlash.setImageResource(
                if (flashEnabled) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            )
        }

        iniciarCamara()
    }

    private fun iniciarCamara() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()

                // Configurar Preview
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                // Configurar ImageAnalysis para escaneo
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                            analizar(imageProxy)
                        }
                    }

                // Seleccionar la cámara trasera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Vincular al ciclo de vida
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {
                Toast.makeText(this, "Error al iniciar la cámara: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizar(imageProxy: ImageProxy) {
        if (procesado) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        BarcodeScanning.getClient().process(inputImage)
            .addOnSuccessListener { barcodes ->
                val codigo = barcodes.firstOrNull()?.rawValue
                if (!codigo.isNullOrBlank() && !procesado) {
                    procesado = true
                    val intent = Intent().putExtra(EXTRA_BARCODE, codigo)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Apagar la linterna si estaba encendida
        if (flashEnabled) {
            camera?.cameraControl?.enableTorch(false)
        }
    }

    companion object {
        const val EXTRA_BARCODE = "extra_barcode"
    }
}