package com.tuapp.ventas.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ActivityMainBinding
import com.tuapp.ventas.ui.cuenta.ActiveAccountActivity
import com.tuapp.ventas.ui.cuenta.NuevoClienteDialog
import com.tuapp.ventas.ui.estadisticas.EstadisticasActivity
import com.tuapp.ventas.ui.main.adapters.CuentasActivasAdapter
import com.tuapp.ventas.ui.main.adapters.VentasRecientesAdapter
import com.tuapp.ventas.ui.scanner.BarcodeScannerActivity
import com.tuapp.ventas.ui.settings.SettingsActivity
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

    private fun configurarListas() {
        binding.recyclerVentas.layoutManager = LinearLayoutManager(this)
        binding.recyclerVentas.adapter = ventasAdapter
        cuentasAdapter = CuentasActivasAdapter { cuenta -> prefs.cuentaSeleccionadaId = cuenta.id; viewModel.seleccionarCuenta(cuenta.id); Toast.makeText(this, "Cuenta #${cuenta.id} seleccionada", Toast.LENGTH_SHORT).show() }
        binding.recyclerCuentas.layoutManager = LinearLayoutManager(this)
        binding.recyclerCuentas.adapter = cuentasAdapter
    }
    private fun configurarClicks() = with(binding) {
        chipSimple.setOnClickListener { cambiarModo(ModoOperacion.SIMPLE) }
        chipCuenta.setOnClickListener { cambiarModo(ModoOperacion.CUENTA) }
        btnEscanearSimple.setOnClickListener { solicitarCamara() }
        btnEscanearCuenta.setOnClickListener { solicitarCamara() }
        btnNuevaCuenta.setOnClickListener { NuevoClienteDialog().apply { onCrear = { n,t -> viewModel.crearCuenta(n,t) } }.show(supportFragmentManager, "nuevo") }
        btnVerCuenta.setOnClickListener { val id = prefs.cuentaSeleccionadaId; if (id > 0) startActivity(Intent(this@MainActivity, ActiveAccountActivity::class.java).putExtra(ActiveAccountActivity.EXTRA_CUENTA_ID, id)) else Toast.makeText(this@MainActivity, "Selecciona una cuenta", Toast.LENGTH_SHORT).show() }
        btnEstadisticas.setOnClickListener { startActivity(Intent(this@MainActivity, EstadisticasActivity::class.java)) }
        btnSettings.setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        btnEntradaManual.setOnClickListener { pedirCodigoManual() }
    }
    private fun observarDatos() {
        viewModel.ventasHoy.observe(this) { ventasAdapter.submitList(it); binding.txtUltimaVenta.text = it.firstOrNull()?.let { v -> "${v.nombreProducto} - ${DateUtils.moneda(v.precio)}" } ?: "Sin ventas todavía" }
        viewModel.totalSimple.observe(this) { binding.txtTotalDia.text = DateUtils.moneda(it) }
        viewModel.cantidadSimple.observe(this) { binding.txtCantidadDia.text = "$it productos" }
        viewModel.cuentasActivas.observe(this) { cuentasAdapter.submitList(it) }
        viewModel.cuentaActual.observe(this) { binding.txtCuentaSeleccionada.text = it?.let { c -> "Cuenta #${c.cuenta.id} · ${c.cliente.nombre} · ${DateUtils.moneda(c.cuenta.total)}" } ?: "Ninguna cuenta seleccionada" }
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
    private fun pedirCodigoManual() { MaterialAlertDialogBuilder(this).setTitle("Código manual").setMessage("Usa 750100000001 para probar Café Americano.").setPositiveButton("Escanear demo") { _, _ -> viewModel.procesarEscaneo("750100000001", modo) }.setNegativeButton("Cancelar", null).show() }
    private fun mostrarDialogoProducto(producto: Producto) { VentaDirectaDialog().apply { this.producto = producto; this.modo = this@MainActivity.modo; onConfirmar = { p,_,_,_,cantidad -> if (modo == ModoOperacion.SIMPLE) viewModel.registrarVentaDirecta(p!!) else viewModel.agregarACuenta(p!!, cantidad) }; onVerCuenta = { binding.btnVerCuenta.performClick() } }.show(supportFragmentManager, "venta") }
    private fun mostrarDialogoNuevoProducto(codigo: String) { VentaDirectaDialog().apply { codigoNuevo = codigo; modo = this@MainActivity.modo; onConfirmar = { _, c, n, precio, cantidad -> if (n.isNullOrBlank() || precio == null) Toast.makeText(this@MainActivity, "Nombre y precio requeridos", Toast.LENGTH_SHORT).show() else viewModel.crearProductoYVender(c!!, n, precio, modo, cantidad) } }.show(supportFragmentManager, "nuevo_producto") }
}
