package com.tuapp.ventas.ui.notificaciones

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivityNotificacionesBinding
import com.tuapp.ventas.ui.base.BaseActivity

class NotificacionesActivity : BaseActivity() {
    private lateinit var binding: ActivityNotificacionesBinding
    private val viewModel: NotificacionesViewModel by viewModels { NotificacionesViewModelFactory((application as VentasApplication).repository) }
    private lateinit var adapter: NotificacionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Notificaciones"
        adapter = NotificacionAdapter(onSeleccionCambio = { actualizarAcciones() }, onEliminar = { viewModel.eliminar(it.id) })
        binding.recyclerNotificaciones.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotificaciones.adapter = adapter
        binding.chkMarcarTodas.setOnCheckedChangeListener { _, checked -> adapter.seleccionarTodas(checked); viewModel.marcarLeidas(adapter.idsSeleccionadas()) }
        binding.btnEliminarSeleccionadas.setOnClickListener {
            val ids = adapter.idsSeleccionadas()
            if (ids.isEmpty()) Toast.makeText(this, "Selecciona al menos una notificación", Toast.LENGTH_SHORT).show() else viewModel.eliminarSeleccionadas(ids)
        }
        viewModel.notificaciones.observe(this) { lista ->
            adapter.submitList(lista)
            binding.txtVacio.text = if (lista.isEmpty()) "Sin notificaciones" else "${lista.size} notificaciones"
            actualizarAcciones()
        }
        viewModel.generar()
    }

    private fun actualizarAcciones() {
        binding.btnEliminarSeleccionadas.isEnabled = adapter.idsSeleccionadas().isNotEmpty()
    }
}
