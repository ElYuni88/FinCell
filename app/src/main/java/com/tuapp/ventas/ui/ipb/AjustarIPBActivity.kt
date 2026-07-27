package com.tuapp.ventas.ui.ipb

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.data.model.Gasto
import com.tuapp.ventas.databinding.ActivityAjustarIpbBinding
import com.tuapp.ventas.databinding.DialogAgregarGastoBinding
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager

class AjustarIPBActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAjustarIpbBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: GastoAjusteAdapter
    private val fecha: String get() = DateUtils.fechaArchivo(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAjustarIpbBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        prefs = PreferencesManager(this)

        configurarRecycler()
        configurarBotones()
    }

    private fun configurarRecycler() {
        adapter = GastoAjusteAdapter(prefs.obtenerGastos(fecha).toMutableList())
        binding.recyclerGastosAjuste.layoutManager = LinearLayoutManager(this)
        binding.recyclerGastosAjuste.adapter = adapter
    }

    private fun configurarBotones() {
        binding.btnAgregarCategoria.setOnClickListener { mostrarDialogoAgregarGasto() }
        binding.btnGuardarAjustes.setOnClickListener {
            prefs.guardarGastos(fecha, adapter.obtenerGastos())
            Toast.makeText(this, "Ajustes guardados", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun mostrarDialogoAgregarGasto() {
        val dialogBinding = DialogAgregarGastoBinding.inflate(LayoutInflater.from(this))
        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar categoría")
            .setView(dialogBinding.root)
            .setPositiveButton("Agregar") { _, _ ->
                val categoria = dialogBinding.etCategoria.text.toString().trim()
                val monto = dialogBinding.etMonto.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                if (categoria.isBlank()) {
                    Toast.makeText(this, "Ingrese una categoría válida", Toast.LENGTH_SHORT).show()
                } else {
                    adapter.agregar(Gasto(categoria = categoria, monto = monto.coerceAtLeast(0.0)))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
