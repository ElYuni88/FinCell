package com.tuapp.ventas.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ActivityMainBinding
import com.tuapp.ventas.ui.common.ProductoNoEncontradoDialogFragment
import com.tuapp.ventas.ui.cuenta.AccountDetailActivity
import com.tuapp.ventas.ui.cuenta.NuevoClienteDialog
import com.tuapp.ventas.ui.estadisticas.EstadisticasActivity
import com.tuapp.ventas.ui.main.adapters.CuentasActivasAdapter
import com.tuapp.ventas.ui.main.adapters.VentasRecientesAdapter
import com.tuapp.ventas.ui.productos.ProductosActivity
import com.tuapp.ventas.ui.exportar.ExportarIPBActivity
import com.tuapp.ventas.ui.scanner.BarcodeScannerActivity
import com.tuapp.ventas.ui.settings.SettingsActivity
import com.tuapp.ventas.ui.simple.AgregarProductoManualDialog
import com.tuapp.ventas.ui.simple.VentaDirectaDialog
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager
import com.tuapp.ventas.utils.SoundUtils

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private var modo = ModoOperacion.SIMPLE
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory((application as VentasApplication).repository) }
    private val ventasAdapter = VentasRecientesAdapter()
    private lateinit var cuentasAdapter: CuentasActivasAdapter
    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val codigo = result.data?.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE).orEmpty()
        if (codigo.isNotBlank()) { if (prefs.sonidoEscaneo) SoundUtils.beep(); if (prefs.vibrarEscaneo) SoundUtils.vibrar(this); viewModel.procesarEscaneo(codigo, modo) }
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) abrirScanner() else Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_LONG).show() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        configurarDrawer()
        prefs = PreferencesManager(this)
        modo = when (prefs.modoPredeterminado) {
            "SIMPLE" -> ModoOperacion.SIMPLE
            "CUENTA" -> ModoOperacion.CUENTA
            else -> prefs.modoActual
        }
        prefs.modoActual = modo
        viewModel.seleccionarCuenta(prefs.cuentaSeleccionadaId)
        configurarListas(); configurarClicks(); observarDatos(); renderModo()
        if (!prefs.tooltipModoMostrado) { Toast.makeText(this, "Cambia entre venta directa y cuentas sin perder datos", Toast.LENGTH_LONG).show(); prefs.tooltipModoMostrado = true }
    }

    private fun configurarDrawer() {
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.app_name, R.string.app_name)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawers()
            manejarNavegacionPrincipal(item.itemId)
        }
    }

    private fun manejarNavegacionPrincipal(itemId: Int): Boolean = when (itemId) {
        R.id.nav_scan -> { solicitarCamara(); true }
        R.id.nav_productos, R.id.menu_productos -> { startActivity(Intent(this, ProductosActivity::class.java)); true }
        R.id.nav_estadisticas, R.id.menu_estadisticas -> { startActivity(Intent(this, EstadisticasActivity::class.java)); true }
        R.id.nav_exportar_ipb, R.id.menu_exportar_ipb -> { confirmarExportarIPB(); true }
        R.id.menu_configuraciones -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        else -> false
    }

    private fun confirmarExportarIPB() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Exportar IPB")
            .setMessage("¿Exportar las operaciones del día en formato IPB?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Exportar") { _, _ -> startActivity(Intent(this, ExportarIPBActivity::class.java)) }
            .show()
    }

    private fun configurarListas() {
        binding.recyclerVentas.layoutManager = LinearLayoutManager(this)
        binding.recyclerVentas.adapter = ventasAdapter
        cuentasAdapter = CuentasActivasAdapter(
            onClick = { cuenta ->
                prefs.cuentaSeleccionadaId = cuenta.id
                viewModel.seleccionarCuenta(cuenta.id)
                startActivity(Intent(this, AccountDetailActivity::class.java).putExtra(AccountDetailActivity.EXTRA_CUENTA_ID, cuenta.id))
            },
            onEliminarCuenta = { cuenta -> confirmarEliminarCuenta(cuenta.id, cuenta.nombreCliente) }
        )
        binding.recyclerCuentas.layoutManager = LinearLayoutManager(this)
        binding.recyclerCuentas.adapter = cuentasAdapter
    }
    private fun configurarClicks() = with(binding) {
        chipSimple.setOnClickListener { cambiarModo(ModoOperacion.SIMPLE) }
        chipCuenta.setOnClickListener { cambiarModo(ModoOperacion.CUENTA) }
        btnEscanearSimple.setOnClickListener { solicitarCamara() }
        btnEscanearCuenta.visibility = View.GONE
        btnVerCuenta.visibility = View.GONE
        txtCuentaSeleccionada.visibility = View.GONE
        btnNuevaCuenta.setOnClickListener { NuevoClienteDialog().apply { onCrear = { n, t, m, r -> viewModel.crearCuenta(n, t, m, r) } }.show(supportFragmentManager, "nuevo") }
        btnAgregarManualSimple.setOnClickListener { mostrarDialogoAgregarManual() }
        bottomNavigation.setOnItemSelectedListener { item -> manejarNavegacionPrincipal(item.itemId) }
    }
    private fun observarDatos() {
        viewModel.ventasHoy.observe(this) { ventasAdapter.submitList(it); binding.txtUltimaVenta.text = it.firstOrNull()?.let { v -> "${v.nombreProducto} - ${DateUtils.moneda(v.precio)}" } ?: "Sin ventas todavía" }
        viewModel.totalSimple.observe(this) { binding.txtTotalDia.text = DateUtils.moneda(it) }
        viewModel.cantidadSimple.observe(this) { binding.txtCantidadDia.text = "$it productos" }
        viewModel.cuentasActivas.observe(this) { cuentasAdapter.submitList(it) }
        viewModel.cantidadCuentasAbiertas.observe(this) { cantidad ->
            binding.txtCuentasAbiertasInfo.text = "Cuentas abiertas: $cantidad"
        }
        viewModel.cuentaActual.observe(this) { cuenta ->
            binding.txtCuentaSeleccionada.text = cuenta?.let { c ->
                val nombre = c.cliente?.nombre ?: c.cuenta.nombreClienteTemporal ?: "Cliente temporal"
                "Cuenta #${c.cuenta.id} · $nombre · ${DateUtils.moneda(c.cuenta.total)}"
            } ?: "Ninguna cuenta seleccionada"
        }
        viewModel.mensaje.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        viewModel.productoEscaneado.observe(this) { it?.let(::mostrarDialogoProducto) }
        viewModel.codigoNuevoProducto.observe(this) { it?.let { codigo -> mostrarDialogoNuevoProducto(codigo) } }
    }
    private fun cambiarModo(nuevo: ModoOperacion) { modo = nuevo; prefs.modoActual = nuevo; renderModo() }
    private fun renderModo() = with(binding) {
        chipSimple.isChecked = modo == ModoOperacion.SIMPLE; chipCuenta.isChecked = modo == ModoOperacion.CUENTA
        panelSimple.visibility = if (modo == ModoOperacion.SIMPLE) View.VISIBLE else View.GONE
        panelCuenta.visibility = if (modo == ModoOperacion.CUENTA) View.VISIBLE else View.GONE
    }
    private fun solicitarCamara() { if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) abrirScanner() else cameraPermission.launch(Manifest.permission.CAMERA) }
    private fun abrirScanner() = scanLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
    override fun onCreateOptionsMenu(menu: Menu): Boolean = false

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_productos -> { startActivity(Intent(this, ProductosActivity::class.java)); true }
        R.id.menu_estadisticas -> { startActivity(Intent(this, EstadisticasActivity::class.java)); true }
        R.id.menu_exportar_ipb -> { confirmarExportarIPB(); true }
        R.id.menu_configuraciones -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun mostrarDialogoAgregarManual() {
        if (modo == ModoOperacion.CUENTA && prefs.cuentaSeleccionadaId <= 0) {
            Toast.makeText(this, "Seleccione una cuenta primero", Toast.LENGTH_SHORT).show()
            return
        }
        AgregarProductoManualDialog().apply {
            onConfirmar = { producto, cantidad -> viewModel.registrarProductoManual(producto, cantidad, modo) }
        }.show(supportFragmentManager, "agregar_manual")
    }

    private fun mostrarDialogoProducto(producto: Producto) { VentaDirectaDialog().apply { this.producto = producto; this.modo = this@MainActivity.modo; onConfirmar = { p,_,_,_,cantidad -> if (modo == ModoOperacion.SIMPLE) viewModel.registrarVentaDirecta(p!!, cantidad) else viewModel.agregarACuenta(p!!, cantidad) }; onVerCuenta = { val id = prefs.cuentaSeleccionadaId; if (id > 0) startActivity(Intent(this@MainActivity, AccountDetailActivity::class.java).putExtra(AccountDetailActivity.EXTRA_CUENTA_ID, id)) } }.show(supportFragmentManager, "venta") }

    private fun confirmarEliminarCuenta(cuentaId: Long, nombreCliente: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("¿Eliminar la cuenta vacía de $nombreCliente?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarCuentaVacia(cuentaId) }
            .show()
    }
    private fun mostrarDialogoNuevoProducto(codigo: String) {
        ProductoNoEncontradoDialogFragment.newInstance(codigo).apply {
            onDarEntrada = { codigoEscaneado -> mostrarDialogoAltaProductoEscaneado(codigoEscaneado) }
        }.show(supportFragmentManager, "producto_no_encontrado")
    }

    private fun mostrarDialogoAltaProductoEscaneado(codigo: String) {
        val dialogBinding = com.tuapp.ventas.databinding.DialogAgregarProductoBinding.inflate(layoutInflater)
        dialogBinding.inputCodigo.setText(codigo)
        dialogBinding.inputCodigo.isEnabled = false
        dialogBinding.inputInventario.hint = "Cantidad en inventario"
        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar producto escaneado")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar y vender") { _, _ ->
                val nombre = dialogBinding.inputNombre.text?.toString()?.trim().orEmpty()
                val precio = dialogBinding.inputPrecio.text?.toString()?.toDoubleOrNull()
                val cantidad = dialogBinding.inputInventario.text?.toString()?.toIntOrNull() ?: 1
                if (nombre.isBlank() || precio == null || precio < 0.0 || cantidad < 0) {
                    Toast.makeText(this, "Nombre, precio y cantidad en inventario válidos son requeridos", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.crearProductoYVender(codigo, nombre, precio, modo, 1, cantidad)
                }
            }
            .show()
    }
}
