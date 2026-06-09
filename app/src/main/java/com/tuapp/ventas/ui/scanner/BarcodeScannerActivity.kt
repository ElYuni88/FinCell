package com.tuapp.ventas.ui.scanner

import android.content.Intent
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tuapp.ventas.databinding.ActivityScannerBinding

class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerBinding
    private var procesado = false
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); binding = ActivityScannerBinding.inflate(layoutInflater); setContentView(binding.root); binding.btnCancelar.setOnClickListener { finish() }; iniciarCamara() }
    private fun iniciarCamara() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { proxy -> analizar(proxy) }
            runCatching { provider.unbindAll(); provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }.onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
        }, ContextCompat.getMainExecutor(this))
    }
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analizar(proxy: androidx.camera.core.ImageProxy) {
        val media = proxy.image ?: run { proxy.close(); return }
        BarcodeScanning.getClient().process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.takeIf { !procesado }?.let { procesado = true; setResult(RESULT_OK, Intent().putExtra(EXTRA_BARCODE, it)); finish() } }
            .addOnCompleteListener { proxy.close() }
    }
    companion object { const val EXTRA_BARCODE = "extra_barcode" }
}
