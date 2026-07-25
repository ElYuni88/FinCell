package com.tuapp.ventas.ui.ipb

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ProductoIPB
import com.tuapp.ventas.databinding.ActivityIpbResumenBinding
import com.tuapp.ventas.ui.exportar.ExportarIPBActivity
import com.tuapp.ventas.utils.DateUtils
import java.io.File

class IPBResumenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIpbResumenBinding
    private val viewModel: IPBResumenViewModel by viewModels {
        IPBResumenViewModelFactory((application as VentasApplication).repository)
    }
    private lateinit var adapter: IPBAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIpbResumenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        configurarRecycler()
        configurarBotones()
        cargarDatos()
    }

    private fun configurarRecycler() {
        adapter = IPBAdapter(emptyList())
        binding.recyclerIPB.layoutManager = LinearLayoutManager(this)
        binding.recyclerIPB.adapter = adapter
    }

    private fun configurarBotones() {
        binding.btnAjustarIPB.setOnClickListener {
            startActivity(Intent(this, AjustarIPBActivity::class.java))
        }

        binding.btnExportar.setOnClickListener {
            val productos = viewModel.productosIPB.value ?: emptyList()
            if (productos.isEmpty()) {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val fecha = DateUtils.fechaArchivo(System.currentTimeMillis())
            val archivo = File(getExternalFilesDir(null), "ipb_$fecha.json")
            val mensaje = if (archivo.exists()) {
                "Ya existe un archivo para la fecha $fecha. ¿Desea sobrescribirlo?"
            } else {
                "¿Exportar IPB para la fecha $fecha?"
            }
            MaterialAlertDialogBuilder(this)
                .setTitle("Exportar IPB")
                .setMessage(mensaje)
                .setPositiveButton("Exportar") { _, _ ->
                    // Llamar a la actividad existente con un flag de sobrescritura
                    val intent = Intent(this, ExportarIPBActivity::class.java).apply {
                        putExtra(ExportarIPBActivity.EXTRA_SOBRESCRIBIR, true)
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun cargarDatos() {
        viewModel.productosIPB.observe(this) { productos ->
            adapter = IPBAdapter(productos)
            binding.recyclerIPB.adapter = adapter
            binding.txtFecha.text = "Fecha: ${DateUtils.fechaHora(System.currentTimeMillis())}"
        }
        viewModel.cargarDatos()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}