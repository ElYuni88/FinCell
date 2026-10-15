package com.tuapp.ventas.ui.ipb

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private var fechaSeleccionada: Long = System.currentTimeMillis()
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

        binding.txtFecha.setOnClickListener { mostrarSelectorFecha() }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
        actualizarBannerEstado()
    }

    // ============================================================
    // BANNER DE ESTADO
    // ============================================================

    private fun configurarBanner() {
        binding.layoutBannerEstado.setOnClickListener {
            when (prefs.estadoPeriodo) {
                EstadoPeriodo.ABIERTO -> {
                    // No hay acción al tocar cuando está abierto en esta pantalla
                    // (el botón "Cerrar y Exportar" ya está visible abajo)
                }
                EstadoPeriodo.CERRADO -> {
                    // Llevar a importar IPV
                    startActivity(Intent(this, ImportarIPVActivity::class.java))
                }
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

    // ============================================================
    // FECHA
    // ============================================================

    private fun mostrarSelectorFecha() {
        val cal = Calendar.getInstance().apply { timeInMillis = fechaSeleccionada }
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val fecha = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (fecha > System.currentTimeMillis()) {
                Toast.makeText(this, "No se pueden ver fechas futuras", Toast.LENGTH_SHORT).show()
                return@DatePickerDialog
            }

            fechaSeleccionada = fecha
            val fechaStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
            binding.txtFecha.text = "Fecha: $fechaStr"

            viewModel.cargarDatos(fecha)

            val esHoy = DateUtils.esMismoDia(fecha, System.currentTimeMillis())
            binding.btnAjustarIPB.isEnabled = esHoy
            binding.btnAjustarIPB.alpha = if (esHoy) 1f else 0.5f

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ============================================================
    // RECYCLERS
    // ============================================================

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

        viewModel.totalVentas.observe(this) {
            binding.txtTotalVentas.text = DateUtils.moneda(it)
        }
        viewModel.totalGastos.observe(this) {
            binding.txtTotalGastos.text = DateUtils.moneda(it)
        }
        viewModel.totalIngresos.observe(this) {
            binding.txtTotalIngresos.text = DateUtils.moneda(it)
        }
        viewModel.totalNeto.observe(this) { total ->
            binding.txtTotalNeto.text = DateUtils.moneda(total)
            val color = if (total >= 0.0) R.color.success else R.color.pos_red
            binding.txtTotalNeto.setTextColor(ContextCompat.getColor(this, color))
        }
    }

    // ============================================================
    // BOTONES
    // ============================================================

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
                putExtra(AjustarIPBActivity.EXTRA_FECHA_SELECCIONADA, fechaSeleccionada)
            }
            startActivity(intent)
        }
        binding.btnExportar.setOnClickListener { confirmarCierreYExportacion() }
    }

    private fun cargarDatos() {
        viewModel.cargarDatos(fechaSeleccionada)
        val fechaStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaSeleccionada)
        binding.txtFecha.text = "Fecha: $fechaStr"

        val esHoy = DateUtils.esMismoDia(fechaSeleccionada, System.currentTimeMillis())
        val estadoAbierto = prefs.estadoPeriodo == EstadoPeriodo.ABIERTO

        // ✅ Ajustar IPB: solo si es HOY Y el estado es ABIERTO
        val puedeAjustar = esHoy && estadoAbierto
        binding.btnAjustarIPB.isEnabled = puedeAjustar
        binding.btnAjustarIPB.alpha = if (puedeAjustar) 1f else 0.5f

        // ✅ Cerrar y Exportar: solo si es HOY Y el estado es ABIERTO
        val puedeCerrar = esHoy && estadoAbierto
        binding.btnExportar.isEnabled = puedeCerrar
        binding.btnExportar.alpha = if (puedeCerrar) 1f else 0.5f
    }

    // ============================================================
    // CERRAR Y EXPORTAR
    // ============================================================

    private fun confirmarCierreYExportacion() {
        // ✅ 1. Validar que el estado sea ABIERTO
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

        // ✅ 2. Validar que la fecha sea HOY
        if (!DateUtils.esMismoDia(fechaSeleccionada, System.currentTimeMillis())) {
            Toast.makeText(this, "Solo puedes cerrar el periodo del día actual", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ 3. Validar cuentas abiertas
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

            // ✅ 4. Todo OK → mostrar diálogo de confirmación
            continuarCierreYExportacion()
        }
    }

    private fun continuarCierreYExportacion() {
        val productos = viewModel.productosIPB.value.orEmpty()
        if (productos.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaStr = DateUtils.fechaArchivo(fechaSeleccionada)
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
            appendLine("📊 Resumen:")
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
                    putExtra(ExportarIPBActivity.EXTRA_FECHA_SELECCIONADA, fechaSeleccionada)
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