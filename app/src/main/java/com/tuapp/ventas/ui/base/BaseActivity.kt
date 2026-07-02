package com.tuapp.ventas.ui.base

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.tuapp.ventas.R
import com.tuapp.ventas.databinding.ActivityBaseBinding
import com.tuapp.ventas.ui.estadisticas.EstadisticasActivity
import com.tuapp.ventas.ui.exportar.ExportarIPBActivity
import com.tuapp.ventas.ui.main.MainActivity
import com.tuapp.ventas.ui.productos.ProductosActivity
import com.tuapp.ventas.ui.settings.SettingsActivity

/**
 * Actividad base de CafePos.
 * Centraliza el DrawerLayout, la Toolbar y el BottomNavigationView para evitar
 * duplicación de layouts y comportamiento entre pantallas.
 */
abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var baseBinding: ActivityBaseBinding
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        super.setContentView(baseBinding.root)
        configurarToolbarYDrawer()
        configurarBottomNavigation()
    }

    /** Redirige el contenido de las hijas al FrameLayout común. */
    override fun setContentView(view: View?) {
        if (::baseBinding.isInitialized && view != baseBinding.root) {
            baseBinding.contentFrame.removeAllViews()
            baseBinding.contentFrame.addView(view)
            seleccionarItemActual()
        } else {
            super.setContentView(view)
        }
    }

    private fun configurarToolbarYDrawer() {
        setSupportActionBar(baseBinding.toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            baseBinding.drawerLayout,
            baseBinding.toolbar,
            R.string.app_name,
            R.string.app_name
        )
        baseBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        baseBinding.navView.setNavigationItemSelectedListener { item ->
            navegar(item.itemId)
            true
        }
    }

    private fun configurarBottomNavigation() {
        baseBinding.bottomNavigation.setOnItemSelectedListener { item ->
            navegar(item.itemId)
            true
        }
    }

    protected fun seleccionarItemActual() {
        val id = when (this) {
            is MainActivity -> R.id.nav_scan
            is ProductosActivity -> R.id.nav_productos
            is EstadisticasActivity -> R.id.nav_estadisticas
            is ExportarIPBActivity -> R.id.nav_exportar_ipb
            else -> 0
        }
        if (id != 0) {
            baseBinding.bottomNavigation.menu.findItem(id)?.isChecked = true
            val drawerId = if (id == R.id.nav_productos) R.id.menu_productos else if (id == R.id.nav_estadisticas) R.id.menu_estadisticas else if (id == R.id.nav_exportar_ipb) R.id.menu_exportar_ipb else id
            baseBinding.navView.menu.findItem(drawerId)?.isChecked = true
        }
    }

    private fun navegar(@IdRes itemId: Int) {
        baseBinding.drawerLayout.closeDrawer(GravityCompat.START)
        when (itemId) {
            R.id.nav_scan -> if (this !is MainActivity) {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
            R.id.nav_productos, R.id.menu_productos -> if (this !is ProductosActivity) startActivity(Intent(this, ProductosActivity::class.java))
            R.id.nav_estadisticas, R.id.menu_estadisticas -> if (this !is EstadisticasActivity) startActivity(Intent(this, EstadisticasActivity::class.java))
            R.id.nav_exportar_ipb, R.id.menu_exportar_ipb -> if (this !is ExportarIPBActivity) startActivity(Intent(this, ExportarIPBActivity::class.java))
            R.id.menu_configuraciones -> if (this !is SettingsActivity) startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
