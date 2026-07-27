package com.tuapp.ventas.ui.ipb

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivityIpbResumenBinding
import com.tuapp.ventas.ui.exportar.ExportarIPBActivity
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager
import java.io.File

class IPBResumenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIpbResumenBinding
    private lateinit var productoAdapter: IPBAdapter
    private lateinit var gastoAdapter: GastoAdapter
    private val fechaActual: Long get() = System.currentTimeMillis()
    private val viewModel: IPBResumenViewModel by viewModels {
        IPBResumenViewModelFactory(
            (application as VentasApplication).repository,
            PreferencesManager(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIpbResumenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        configurarRecyclers()
        configurarObservadores()
        configurarBotones()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun configurarRecyclers() {
        productoAdapter = IPBAdapter(emptyList())
        gastoAdapter = GastoAdapter(emptyList())
        binding.recyclerIPB.layoutManager = LinearLayoutManager(this)
        binding.recyclerIPB.adapter = productoAdapter
        binding.recyclerGastos.layoutManager = LinearLayoutManager(this)
        binding.recyclerGastos.adapter = gastoAdapter
    }

    private fun configurarObservadores() {
        viewModel.productosIPB.observe(this) { productoAdapter.submitList(it) }
        viewModel.gastos.observe(this) { gastoAdapter.submitList(it) }
        viewModel.totalVentas.observe(this) { binding.txtTotalVentas.text = "Total ventas: ${DateUtils.moneda(it)}" }
        viewModel.totalGastos.observe(this) { binding.txtTotalGastos.text = "Total gastos: ${DateUtils.moneda(it)}" }
        viewModel.totalNeto.observe(this) { total ->
            binding.txtTotalNeto.text = "Total neto: ${DateUtils.moneda(total)}"
            val color = if (total >= 0.0) R.color.success else R.color.pos_red
            binding.txtTotalNeto.setTextColor(ContextCompat.getColor(this, color))
        }
    }

    private fun configurarBotones() {
        binding.btnAjustarIPB.setOnClickListener {
            startActivity(Intent(this, AjustarIPBActivity::class.java))
        }
        binding.btnExportar.setOnClickListener { confirmarExportacion() }
    }

    private fun cargarDatos() {
        binding.txtFecha.text = "Fecha: ${DateUtils.fechaHora(fechaActual)}"
        viewModel.cargarDatos(fechaActual)
    }

    private fun confirmarExportacion() {
        val productos = viewModel.productosIPB.value.orEmpty()
        if (productos.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }
        val fecha = DateUtils.fechaArchivo(fechaActual)
        val archivoApp = File(getExternalFilesDir(null), "ipb_$fecha.json")
        val gastos = viewModel.gastos.value.orEmpty()
        val resumenGastos = if (gastos.isEmpty()) {
            "Gastos registrados: ninguno"
        } else {
            gastos.joinToString(separator = "\n") { "• ${it.categoria}: ${DateUtils.moneda(it.monto)}" }
        }
        val mensajeBase = if (archivoApp.exists()) {
            "Ya existe un archivo para la fecha $fecha. ¿Desea sobrescribirlo?"
        } else {
            "¿Exportar IPB para la fecha $fecha?"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Exportar IPB")
            .setMessage("$mensajeBase\n\n$resumenGastos\n\nTotal gastos: ${DateUtils.moneda(viewModel.totalGastos.value ?: 0.0)}")
            .setPositiveButton("Exportar") { _, _ ->
                val intent = Intent(this, ExportarIPBActivity::class.java).apply {
                    putExtra(ExportarIPBActivity.EXTRA_SOBRESCRIBIR, true)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
