package com.tuapp.ventas.ui.estadisticas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivityEstadisticasBinding
import com.tuapp.ventas.ui.exportar.ExportarIPBActivity
import com.tuapp.ventas.ui.main.MainActivity
import com.tuapp.ventas.ui.productos.ProductosActivity
import com.tuapp.ventas.utils.DateUtils

class EstadisticasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEstadisticasBinding

    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) viewModel.exportar()
        else Toast.makeText(this, "Permiso de escritura requerido en Android 9 o inferior", Toast.LENGTH_LONG).show()
    }

    private val viewModel: EstadisticasViewModel by viewModels {
        EstadisticasViewModelFactory((application as VentasApplication).repository, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        binding.btnVolver.setOnClickListener { finish() }
        binding.btnExportar.setOnClickListener { exportarConPermisos() }
        binding.bottomNavigation.selectedItemId = R.id.nav_estadisticas
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan -> { startActivity(Intent(this, MainActivity::class.java)); true }
                R.id.nav_productos -> { startActivity(Intent(this, ProductosActivity::class.java)); true }
                R.id.nav_estadisticas -> true
                R.id.nav_exportar_ipb -> { startActivity(Intent(this, ExportarIPBActivity::class.java)); true }
                else -> false
            }
        }
        observar()
    }

    private fun exportarConPermisos() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.exportar()
        }
    }

    private fun observar() {
        viewModel.totalSimple.observe(this) {
            binding.txtTotalSimple.text = DateUtils.moneda(it)
            actualizarGrafico()
        }

        viewModel.cantidadSimple.observe(this) {
            binding.txtCantidadSimple.text = "$it ventas"
            binding.btnExportar.isEnabled = it > 0 || (viewModel.cantidadCuentas.value ?: 0) > 0
        }

        viewModel.totalCuentas.observe(this) {
            binding.txtTotalCuentas.text = DateUtils.moneda(it)
            actualizarGrafico()
        }

        viewModel.cantidadCuentas.observe(this) {
            binding.txtCantidadCuentas.text = "$it cuentas"
            binding.btnExportar.isEnabled = it > 0 || (viewModel.cantidadSimple.value ?: 0) > 0
        }

        viewModel.cantidadCuentasAbiertas.observe(this) { binding.txtCuentasAbiertas.text = "$it abiertas" }
        viewModel.productosAgotados.observe(this) { binding.txtProductosAgotados.text = "$it agotados" }

        viewModel.ventasHoy.observe(this) { ventas ->
            binding.txtTopProductos.text = ventas
                .groupingBy { it.nombreProducto }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .joinToString("\n") { "${it.key}: ${it.value}" }
                .ifBlank { "Sin ventas simples; las cuentas cerradas se incluyen en exportación." }
        }

        // ✅ ACTUALIZADO: Maneja Result<Uri> en lugar de ruta String
        viewModel.exportacion.observe(this) { result ->
            result.onSuccess { uri ->
                android.app.AlertDialog.Builder(this)
                    .setTitle("Exportación segura creada")
                    .setMessage("Archivo guardado correctamente")
                    .setPositiveButton("Compartir") { _, _ -> compartirArchivo(uri) }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }.onFailure {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ✅ NUEVO MÉTODO: Compartir archivo usando URI
    private fun compartirArchivo(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir archivo de ventas"))
    }

    private fun actualizarGrafico() {
        val s = viewModel.totalSimple.value ?: 0.0
        val c = viewModel.totalCuentas.value ?: 0.0
        binding.chart.data = BarData(BarDataSet(
            listOf(BarEntry(0f, s.toFloat()), BarEntry(1f, c.toFloat())),
            "Simple vs Cuentas"
        ))
        binding.chart.invalidate()
    }
}