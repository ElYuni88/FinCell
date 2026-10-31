package com.tuapp.ventas.ui.ipb

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivityIpbAnalisisBinding
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager

class IPBAnalisisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIpbAnalisisBinding
    private lateinit var adapter: ProductoAnalisisAdapter
    private val viewModel: IPBAnalisisViewModel by viewModels {
        IPBAnalisisViewModelFactory(
            (application as VentasApplication).repository,
            PreferencesManager(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIpbAnalisisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Adapter con callback de clic en celda
        adapter = ProductoAnalisisAdapter { fila, columna ->
            adapter.toggleSeleccion(fila, columna)
            actualizarHeadersSeleccion(adapter.columnaSeleccionada)
        }

        binding.recyclerAnalisis.setHasFixedSize(true)
        binding.recyclerAnalisis.layoutManager = LinearLayoutManager(this)
        binding.recyclerAnalisis.adapter = adapter

        binding.btnImprimir.setOnClickListener {
            Toast.makeText(this, "La impresión estará disponible próximamente", Toast.LENGTH_SHORT).show()
        }
        binding.btnCerrar.setOnClickListener { finish() }

        binding.btnVerGastos.setOnClickListener {
            val gastos = viewModel.gastos.value.orEmpty()
            GastosDesgloseDialogFragment.newInstance(gastos)
                .show(supportFragmentManager, "gastos_desglose")
        }

        viewModel.rangoTexto.observe(this) { binding.txtRango.text = it }

        viewModel.productos.observe(this) { lista ->
            adapter.submitList(lista)
            val vacia = lista.isEmpty()
            binding.txtVacio.visibility = if (vacia) View.VISIBLE else View.GONE
            binding.recyclerAnalisis.visibility = if (vacia) View.GONE else View.VISIBLE
        }

        viewModel.gastos.observe(this) { lista ->
            binding.btnVerGastos.text = "💸 Ver gastos desglosados (${lista.size})"
            binding.btnVerGastos.isEnabled = lista.isNotEmpty()
        }

        viewModel.totales.observe(this) { t ->
            binding.txtTotalVentas.text = DateUtils.moneda(t.totalVentas)
            binding.txtTotalGastos.text = "-" + DateUtils.moneda(t.totalGastos)
            binding.txtTotalNeto.text = DateUtils.moneda(t.totalNeto)
            binding.txtTotalNeto.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (t.totalNeto >= 0.0) R.color.pos_green else R.color.colorError
                )
            )
            binding.txtTotalVendidos.text = "${t.totalVendidos} vendidos"
        }

        // Estado inicial: ningún header resaltado
        actualizarHeadersSeleccion(null)

        viewModel.cargar()
    }

    /** Actualiza el background de los headers según la columna seleccionada. */
    private fun actualizarHeadersSeleccion(columna: Int?) {
        val colorNormal = ContextCompat.getColor(this, R.color.surface)
        val colorSel = ContextCompat.getColor(this, R.color.analisis_highlight_header)
        val colorTextoNormal = ContextCompat.getColor(this, R.color.pos_green)
        val colorTextoSeleccionado = ContextCompat.getColor(this, R.color.white)

        val headers: List<TextView> = listOf(
            binding.headerProducto,
            binding.headerEntrada,
            binding.headerStockIni,
            binding.headerVendidos,
            binding.headerStockFin,
            binding.headerSubtotal
        )

        headers.forEachIndexed { idx, tv ->
            val seleccionada = columna == idx
            tv.setBackgroundColor(if (seleccionada) colorSel else colorNormal)
            tv.setTextColor(if (seleccionada) colorTextoSeleccionado else colorTextoNormal)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}