package com.tuapp.ventas.ui.estadisticas

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivityEstadisticasBinding
import com.tuapp.ventas.ui.base.BaseActivity
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager
import java.util.concurrent.TimeUnit

class EstadisticasActivity : BaseActivity() {
    private lateinit var binding: ActivityEstadisticasBinding
    private lateinit var adapter: EstadisticasPagerAdapter

    private val viewModel: EstadisticasViewModel by viewModels {
        EstadisticasViewModelFactory((application as VentasApplication).repository, applicationContext)
    }

    override fun shouldShowBaseToolbar(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ocultarBarraBaseConFade()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        configurarTabs()
        actualizarSubtitle()
    }

    private fun ocultarBarraBaseConFade() {
        val duracion = 150L
        baseBinding.toolbar.animate().alpha(0f).setDuration(duracion).withEndAction {
            baseBinding.toolbar.visibility = View.GONE
            baseBinding.toolbar.alpha = 1f
        }.start()
        baseBinding.bottomNavigation.animate().alpha(0f).setDuration(duracion).withEndAction {
            baseBinding.bottomNavigation.visibility = View.GONE
            baseBinding.bottomNavigation.alpha = 1f
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // ✅ Ya no hay selector de fecha: no inflamos menú
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun configurarTabs() {
        adapter = EstadisticasPagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Resumen del período"
                1 -> "Existencias del período"
                else -> ""
            }
        }.attach()
    }

    /** Subtitle fijo: "Desde dd/MM/yyyy HH:mm (N días)" */
    private fun actualizarSubtitle() {
        val prefs = PreferencesManager(this)
        val inicio = prefs.obtenerInicioPeriodoVentas().takeIf { it > 0L }
            ?: DateUtils.inicioDia(System.currentTimeMillis())
        val fin = System.currentTimeMillis()
        val dias = (TimeUnit.MILLISECONDS.toDays(fin - inicio) + 1).coerceAtLeast(1)
        val diasTexto = if (dias == 1L) "1 día" else "$dias días"
        binding.toolbar.subtitle = "Desde ${DateUtils.fechaHora(inicio)} ($diasTexto)"
    }
}