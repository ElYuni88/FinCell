package com.tuapp.ventas.ui.estadisticas

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivityEstadisticasBinding
import com.tuapp.ventas.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EstadisticasActivity : BaseActivity() {
    private lateinit var binding: ActivityEstadisticasBinding
    private lateinit var adapter: EstadisticasPagerAdapter

    private val viewModel: EstadisticasViewModel by viewModels {
        EstadisticasViewModelFactory((application as VentasApplication).repository, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        configurarTabs()
        configurarFecha()
        observarDatos()
    }

    private fun configurarTabs() {
        adapter = EstadisticasPagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Resumen del día"
                1 -> "Existencia del día"
                else -> ""
            }
        }.attach()
    }

    private fun configurarFecha() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.menu_calendario) {
                mostrarSelectorFecha()
                true
            } else {
                false
            }
        }
    }

    private fun observarDatos() {
        viewModel.fechaSeleccionada.observe(this) { fecha ->
            binding.toolbar.subtitle = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
        }
    }

    private fun mostrarSelectorFecha() {
        val calendario = Calendar.getInstance().apply {
            timeInMillis = viewModel.fechaSeleccionada.value ?: System.currentTimeMillis()
        }
        DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                val seleccion = Calendar.getInstance().apply {
                    set(Calendar.YEAR, anio)
                    set(Calendar.MONTH, mes)
                    set(Calendar.DAY_OF_MONTH, dia)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.seleccionarFecha(seleccion.timeInMillis)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
