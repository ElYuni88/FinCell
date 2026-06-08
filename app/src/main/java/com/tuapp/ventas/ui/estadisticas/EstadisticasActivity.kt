package com.tuapp.ventas.ui.estadisticas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivityEstadisticasBinding
import com.tuapp.ventas.utils.DateUtils

class EstadisticasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEstadisticasBinding
    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) viewModel.exportar() else Toast.makeText(this, "Permiso de escritura requerido en Android 9 o inferior", Toast.LENGTH_LONG).show() }
    private val viewModel: EstadisticasViewModel by viewModels { EstadisticasViewModelFactory((application as VentasApplication).repository, applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); binding = ActivityEstadisticasBinding.inflate(layoutInflater); setContentView(binding.root); binding.btnVolver.setOnClickListener { finish() }; binding.btnExportar.setOnClickListener { exportarConPermisos() }; observar() }
    private fun exportarConPermisos() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else viewModel.exportar()
    }
    private fun observar() {
        viewModel.totalSimple.observe(this) { binding.txtTotalSimple.text = DateUtils.moneda(it); actualizarGrafico() }
        viewModel.cantidadSimple.observe(this) { binding.txtCantidadSimple.text = "$it ventas"; binding.btnExportar.isEnabled = it > 0 || (viewModel.cantidadCuentas.value ?: 0) > 0 }
        viewModel.totalCuentas.observe(this) { binding.txtTotalCuentas.text = DateUtils.moneda(it); actualizarGrafico() }
        viewModel.cantidadCuentas.observe(this) { binding.txtCantidadCuentas.text = "$it cuentas"; binding.btnExportar.isEnabled = it > 0 || (viewModel.cantidadSimple.value ?: 0) > 0 }
        viewModel.ventasHoy.observe(this) { ventas -> binding.txtTopProductos.text = ventas.groupingBy { it.nombreProducto }.eachCount().entries.sortedByDescending { it.value }.take(5).joinToString("\n") { "${it.key}: ${it.value}" }.ifBlank { "Sin ventas simples; las cuentas cerradas se incluyen en exportación." } }
        viewModel.exportacion.observe(this) { r -> r.onSuccess { ruta -> android.app.AlertDialog.Builder(this).setTitle("Exportación segura creada").setMessage(ruta).setPositiveButton("Compartir") { _, _ -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/octet-stream").putExtra(Intent.EXTRA_TEXT, ruta), "Compartir")) }.setNegativeButton("Cerrar", null).show() }.onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() } }
    }
    private fun actualizarGrafico() { val s = viewModel.totalSimple.value ?: 0.0; val c = viewModel.totalCuentas.value ?: 0.0; binding.chart.data = BarData(BarDataSet(listOf(BarEntry(0f, s.toFloat()), BarEntry(1f, c.toFloat())), "Simple vs Cuentas")); binding.chart.invalidate() }
}
