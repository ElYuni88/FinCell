package com.tuapp.ventas.ui.ipb

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.EstadoPeriodo
import com.tuapp.ventas.databinding.ActivityIpbResumenBinding
import com.tuapp.ventas.ui.exportar.ExportarIPBActivity
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class IPBResumenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIpbResumenBinding
    private lateinit var productoAdapter: IPBAdapter
    private lateinit var gastoAdapter: GastoAdapter
    private lateinit var ingresoAdapter: GastoAdapter
    private val viewModel: IPBResumenViewModel by viewModels {
        IPBResumenViewModelFactory(
            (application as VentasApplication).repository,
            PreferencesManager(this)
        )
    }
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIpbResumenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        configurarRecyclers()
        configurarObservadores()
        configurarBotones()
        configurarBanner()

        // ✅ Fecha ya no es seleccionable: mostramos el rango del periodo
        binding.txtFecha.setOnClickListener(null)
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
        actualizarBannerEstado()
    }

    private fun configurarBanner() {
        binding.layoutBannerEstado.setOnClickListener {
            when (prefs.estadoPeriodo) {
                EstadoPeriodo.ABIERTO -> Unit
                EstadoPeriodo.CERRADO -> startActivity(Intent(this, ImportarIPVActivity::class.java))
            }
        }
    }

    private fun actualizarBannerEstado() {
        when (prefs.estadoPeriodo) {
            EstadoPeriodo.ABIERTO -> {
                binding.layoutBannerEstado.setBackgroundResource(R.drawable.bg_banner_periodo_abierto)
                binding.txtBannerEstado.text = "✅ PERIODO ABIERTO"
                binding.txtBannerAccion.text = ""
            }
            EstadoPeriodo.CERRADO -> {
                binding.layoutBannerEstado.setBackgroundResource(R.drawable.bg_banner_periodo_cerrado)
                binding.txtBannerEstado.text = "🔒 PERIODO CERRADO"
                binding.txtBannerAccion.text = "Pendiente importar IPV →"
            }
        }
    }

    private fun configurarRecyclers() {
        productoAdapter = IPBAdapter(emptyList())
        gastoAdapter = GastoAdapter(emptyList())
        ingresoAdapter = GastoAdapter(emptyList())

        binding.recyclerIPB.setHasFixedSize(true)
        binding.recyclerIPB.layoutManager = LinearLayoutManager(this)
        binding.recyclerIPB.adapter = productoAdapter

        binding.recyclerGastos.setHasFixedSize(true)
        binding.recyclerGastos.layoutManager = LinearLayoutManager(this)
        binding.recyclerGastos.adapter = gastoAdapter

        binding.recyclerIngresos.setHasFixedSize(true)
        binding.recyclerIngresos.layoutManager = LinearLayoutManager(this)
        binding.recyclerIngresos.adapter = ingresoAdapter
    }

    private fun configurarObservadores() {
        viewModel.productosIPB.observe(this) { productoAdapter.submitList(it) }
        viewModel.gastos.observe(this) { gastoAdapter.submitList(it) }
        viewModel.ingresos.observe(this) { ingresoAdapter.submitList(it) }

        viewModel.totalVentas.observe(this) { binding.txtTotalVentas.text = DateUtils.moneda(it) }
        viewModel.totalGastos.observe(this) { binding.txtTotalGastos.text = DateUtils.moneda(it) }
        viewModel.totalIngresos.observe(this) { binding.txtTotalIngresos.text = DateUtils.moneda(it) }
        viewModel.totalNeto.observe(this) { total ->
            binding.txtTotalNeto.text = DateUtils.moneda(total)
            val color = if (total >= 0.0) R.color.success else R.color.pos_red
            binding.txtTotalNeto.setTextColor(ContextCompat.getColor(this, color))
        }
    }

    private fun configurarBotones() {
        binding.btnAjustarIPB.setOnClickListener {
            if (prefs.estadoPeriodo == EstadoPeriodo.CERRADO) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("🔒 Periodo cerrado")
                    .setMessage(
                        "No puedes ajustar los gastos e ingresos mientras el periodo esté cerrado.\n\n" +
                                "El periodo se reabre al importar el IPV del admin."
                    )
                    .setPositiveButton("Entendido", null)
                    .show()
                return@setOnClickListener
            }
            val intent = Intent(this, AjustarIPBActivity::class.java).apply {
                putExtra(AjustarIPBActivity.EXTRA_FECHA_SELECCIONADA, System.currentTimeMillis())
            }
            startActivity(intent)
        }
        binding.btnExportar.setOnClickListener { confirmarCierreYExportacion() }
    }

    private fun cargarDatos() {
        viewModel.cargarDatos()

        // ✅ Mostrar rango del periodo en lugar de una sola fecha
        val inicio = prefs.obtenerInicioPeriodoVentas().takeIf { it > 0L }
            ?: DateUtils.inicioDia(System.currentTimeMillis())
        val fin = System.currentTimeMillis()
        val dias = (TimeUnit.MILLISECONDS.toDays(fin - inicio) + 1).coerceAtLeast(1)
        val inicioStr = DateUtils.fechaHora(inicio).substringBefore(" ")
        val diasTexto = if (dias == 1L) "1 día" else "$dias días"
        binding.txtFecha.text = "Desde $inicioStr hasta hoy ($diasTexto)"

        val estadoAbierto = prefs.estadoPeriodo == EstadoPeriodo.ABIERTO
        binding.btnAjustarIPB.isEnabled = estadoAbierto
        binding.btnAjustarIPB.alpha = if (estadoAbierto) 1f else 0.5f
        binding.btnExportar.isEnabled = estadoAbierto
        binding.btnExportar.alpha = if (estadoAbierto) 1f else 0.5f
    }

    private fun confirmarCierreYExportacion() {
        if (prefs.estadoPeriodo != EstadoPeriodo.ABIERTO) {
            MaterialAlertDialogBuilder(this)
                .setTitle("🔒 Periodo cerrado")
                .setMessage(
                    "El periodo ya está cerrado.\n\n" +
                            "Debes importar el IPV del admin para reabrir el periodo.\n\n" +
                            "Si te equivocaste al cerrar, puedes revertir el cierre desde:\n" +
                            "Menú → Configuraciones → Revertir cierre"
                )
                .setPositiveButton("Ir a importar") { _, _ ->
                    startActivity(Intent(this, ImportarIPVActivity::class.java))
                }
                .setNegativeButton("Entendido", null)
                .show()
            return
        }

        lifecycleScope.launch {
            val repo = (application as VentasApplication).repository
            val cuentasAbiertas = repo.cantidadCuentasAbiertas().firstOrNull() ?: 0

            if (cuentasAbiertas > 0) {
                MaterialAlertDialogBuilder(this@IPBResumenActivity)
                    .setTitle("⚠️ No se puede cerrar")
                    .setMessage(
                        "Hay $cuentasAbiertas cuenta(s) abierta(s) en este momento.\n\n" +
                                "Debes cerrar TODAS las cuentas antes de cerrar el periodo.\n\n" +
                                "Ve a Inicio y cierra las cuentas pendientes."
                    )
                    .setPositiveButton("Entendido", null)
                    .show()
                return@launch
            }

            continuarCierreYExportacion()
        }
    }

    private fun continuarCierreYExportacion() {
        val productos = viewModel.productosIPB.value.orEmpty()
        if (productos.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaStr = DateUtils.fechaArchivo(System.currentTimeMillis())
        val archivoApp = File(getExternalFilesDir(null), "resumen_ipb_${fechaStr}.json")

        val totalVentas = viewModel.totalVentas.value ?: 0.0
        val totalGastos = viewModel.totalGastos.value ?: 0.0
        val totalIngresos = viewModel.totalIngresos.value ?: 0.0
        val totalNeto = viewModel.totalNeto.value ?: 0.0

        val mensaje = buildString {
            if (archivoApp.exists()) {
                appendLine("⚠️ Ya existe un archivo para $fechaStr.")
                appendLine("¿Deseas sobrescribirlo?")
            } else {
                appendLine("¿Cerrar el periodo y exportar el IPB?")
            }
            appendLine()
            appendLine("📊 Resumen del periodo:")
            appendLine("• Total ventas: ${DateUtils.moneda(totalVentas)}")
            appendLine("• Total gastos: ${DateUtils.moneda(totalGastos)}")
            appendLine("• Otros ingresos: ${DateUtils.moneda(totalIngresos)}")
            appendLine("─────────────────")
            appendLine("• Total neto: ${DateUtils.moneda(totalNeto)}")
            appendLine()
            appendLine("⚠️ Al cerrar el periodo NO podrás exportar de nuevo hasta importar el IPV del admin.")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("🔒 Cerrar y Exportar")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar y Exportar") { _, _ ->
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