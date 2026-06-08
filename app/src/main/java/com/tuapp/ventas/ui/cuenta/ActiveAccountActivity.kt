package com.tuapp.ventas.ui.cuenta

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivityActiveAccountBinding
import com.tuapp.ventas.ui.cuenta.adapters.DetalleCuentaAdapter
import com.tuapp.ventas.utils.DateUtils

class ActiveAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActiveAccountBinding
    private val cuentaId by lazy { intent.getLongExtra(EXTRA_CUENTA_ID, -1) }
    private val viewModel: ActiveAccountViewModel by viewModels { ActiveAccountViewModelFactory((application as VentasApplication).repository, cuentaId) }
    private val adapter = DetalleCuentaAdapter()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); binding = ActivityActiveAccountBinding.inflate(layoutInflater); setContentView(binding.root); binding.recyclerDetalle.layoutManager = LinearLayoutManager(this); binding.recyclerDetalle.adapter = adapter; binding.btnVolver.setOnClickListener { finish() }; binding.btnCerrarCuenta.setOnClickListener { CerrarCuentaDialog().apply { total = binding.txtTotal.text.toString().removePrefix("Total: $").toDoubleOrNull() ?: 0.0; onCerrar = { m,o -> viewModel.cerrarCuenta(m,o) } }.show(supportFragmentManager, "cerrar") }; observar() }
    private fun observar() { viewModel.cuenta.observe(this) { c -> if (c != null) { binding.txtCliente.text = c.cliente.nombre; binding.txtTotal.text = "Total: ${DateUtils.moneda(c.cuenta.total)}"; adapter.submitList(c.detalles); binding.btnCerrarCuenta.isEnabled = c.cuenta.estado == "ABIERTA" } }; viewModel.mensaje.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show(); finish() } }
    companion object { const val EXTRA_CUENTA_ID = "cuenta_id" }
}
