package com.tuapp.ventas.ui.ipb

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.tuapp.ventas.data.model.AjusteDia
import com.tuapp.ventas.data.model.CategoriaAjuste
import com.tuapp.ventas.databinding.ActivityAjustarIpbBinding
import com.tuapp.ventas.databinding.DialogAgregarGastoBinding
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager

class AjustarIPBActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAjustarIpbBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: AjusteAdapter

    // ✅ CAMBIO 1: Recibir la fecha por Intent
    private val fechaSeleccionada: Long by lazy {
        intent.getLongExtra(EXTRA_FECHA_SELECCIONADA, System.currentTimeMillis())
    }
    private val fecha: String get() = DateUtils.fechaArchivo(fechaSeleccionada)

    // Tipo actual: GASTO o INGRESO
    private var tipoActual: String = CategoriaAjuste.TIPO_GASTO

    // Cache local de ajustes de gastos e ingresos
    private var ajustesGastos: MutableList<AjusteDia> = mutableListOf()
    private var ajustesIngresos: MutableList<AjusteDia> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAjustarIpbBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Ajustar IPV"
        prefs = PreferencesManager(this)

        // ✅ CAMBIO 2: Log para verificar qué fecha está usando
        android.util.Log.d("AjustarIPB", "Fecha seleccionada: $fecha ($fechaSeleccionada)")

        cargarTodosLosAjustes()
        configurarTabs()
        configurarRecycler()
        configurarBotones()
        mostrarAjustesDelTipoActual()
    }

    private fun cargarTodosLosAjustes() {
        val categorias = prefs.obtenerCategorias()
        val ajustesDia = prefs.obtenerAjustesDia(fecha)

        // ✅ CAMBIO 3: Log para ver qué ajustes encontró
        android.util.Log.d("AjustarIPB", "Categorías totales: ${categorias.size}")
        android.util.Log.d("AjustarIPB", "Ajustes guardados en '$fecha': ${ajustesDia.size}")
        ajustesDia.forEach {
            android.util.Log.d("AjustarIPB", "  - ${it.nombre} (${it.tipo}) = ${it.monto} activo=${it.activo}")
        }

        // Cargar gastos
        val categoriasGastos = categorias.filter { it.tipo == CategoriaAjuste.TIPO_GASTO }
        val ajustesGastosGuardados = ajustesDia.filter { it.tipo == CategoriaAjuste.TIPO_GASTO }
        ajustesGastos = categoriasGastos.map { cat ->
            ajustesGastosGuardados.find { it.categoriaId == cat.id }
                ?: AjusteDia(
                    categoriaId = cat.id,
                    nombre = cat.nombre,
                    tipo = cat.tipo,
                    monto = cat.montoSugerido,
                    activo = false
                )
        }.toMutableList()

        // Cargar ingresos
        val categoriasIngresos = categorias.filter { it.tipo == CategoriaAjuste.TIPO_INGRESO }
        val ajustesIngresosGuardados = ajustesDia.filter { it.tipo == CategoriaAjuste.TIPO_INGRESO }
        ajustesIngresos = categoriasIngresos.map { cat ->
            ajustesIngresosGuardados.find { it.categoriaId == cat.id }
                ?: AjusteDia(
                    categoriaId = cat.id,
                    nombre = cat.nombre,
                    tipo = cat.tipo,
                    monto = cat.montoSugerido,
                    activo = false
                )
        }.toMutableList()
    }

    private fun mostrarAjustesDelTipoActual() {
        val lista = if (tipoActual == CategoriaAjuste.TIPO_GASTO) {
            ajustesGastos
        } else {
            ajustesIngresos
        }
        adapter.submitList(lista.toList())
    }

    private fun configurarTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("💸 Gastos"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("💰 Ingresos"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                guardarCambiosDelAdapter()
                tipoActual = when (tab?.position) {
                    0 -> CategoriaAjuste.TIPO_GASTO
                    1 -> CategoriaAjuste.TIPO_INGRESO
                    else -> CategoriaAjuste.TIPO_GASTO
                }
                mostrarAjustesDelTipoActual()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun guardarCambiosDelAdapter() {
        val ajustesActuales = adapter.obtenerAjustes()
        if (tipoActual == CategoriaAjuste.TIPO_GASTO) {
            ajustesGastos = ajustesActuales.toMutableList()
        } else {
            ajustesIngresos = ajustesActuales.toMutableList()
        }
    }

    private fun configurarRecycler() {
        adapter = AjusteAdapter(
            onMontoCambiado = { },
            onActivoCambiado = { _, _ -> },
            onEliminar = { ajuste -> confirmarEliminarCategoria(ajuste) }
        )
        binding.recyclerGastosAjuste.layoutManager = LinearLayoutManager(this)
        binding.recyclerGastosAjuste.adapter = adapter
    }

    private fun configurarBotones() {
        binding.btnAgregarCategoria.setOnClickListener { mostrarDialogoAgregarCategoria() }
        binding.btnGuardarAjustes.setOnClickListener { guardarTodosLosAjustes() }
    }

    private fun guardarTodosLosAjustes() {
        guardarCambiosDelAdapter()
        val todosLosAjustes = ajustesGastos + ajustesIngresos

        // ✅ CAMBIO 4: Log para ver qué se está guardando
        android.util.Log.d("AjustarIPB", "=== GUARDANDO ===")
        android.util.Log.d("AjustarIPB", "Fecha: '$fecha'")
        android.util.Log.d("AjustarIPB", "Total ajustes: ${todosLosAjustes.size}")
        todosLosAjustes.forEach {
            android.util.Log.d("AjustarIPB", "  ${it.nombre} (${it.tipo}): ${it.monto} activo=${it.activo}")
        }

        prefs.guardarAjustesDia(fecha, todosLosAjustes)

        Toast.makeText(this, "Ajustes guardados para $fecha", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun mostrarDialogoAgregarCategoria() {
        val dialogBinding = DialogAgregarGastoBinding.inflate(LayoutInflater.from(this))
        val titulo = if (tipoActual == CategoriaAjuste.TIPO_GASTO) {
            "Agregar categoría de gasto"
        } else {
            "Agregar categoría de ingreso"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setView(dialogBinding.root)
            .setPositiveButton("Agregar") { _, _ ->
                val nombre = dialogBinding.etCategoria.text.toString().trim()
                val monto = dialogBinding.etMonto.text.toString()
                    .replace(",", ".")
                    .toDoubleOrNull() ?: 0.0

                if (nombre.isBlank()) {
                    Toast.makeText(this, "Ingrese un nombre válido", Toast.LENGTH_SHORT).show()
                } else {
                    val nuevaCategoria = CategoriaAjuste(
                        nombre = nombre,
                        tipo = tipoActual,
                        montoSugerido = monto.coerceAtLeast(0.0)
                    )
                    prefs.agregarCategoria(nuevaCategoria)

                    val nuevoAjuste = AjusteDia(
                        categoriaId = nuevaCategoria.id,
                        nombre = nuevaCategoria.nombre,
                        tipo = nuevaCategoria.tipo,
                        monto = nuevaCategoria.montoSugerido,
                        activo = false
                    )
                    if (tipoActual == CategoriaAjuste.TIPO_GASTO) {
                        ajustesGastos.add(nuevoAjuste)
                    } else {
                        ajustesIngresos.add(nuevoAjuste)
                    }

                    mostrarAjustesDelTipoActual()
                    Toast.makeText(this, "Categoría agregada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminarCategoria(ajuste: AjusteDia) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar categoría")
            .setMessage("¿Eliminar '${ajuste.nombre}' de las categorías persistentes?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                prefs.eliminarCategoria(ajuste.categoriaId)
                if (ajuste.tipo == CategoriaAjuste.TIPO_GASTO) {
                    ajustesGastos.removeAll { it.categoriaId == ajuste.categoriaId }
                } else {
                    ajustesIngresos.removeAll { it.categoriaId == ajuste.categoriaId }
                }
                mostrarAjustesDelTipoActual()
                Toast.makeText(this, "Categoría eliminada", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ✅ CAMBIO 5: Agregar la constante EXTRA_FECHA_SELECCIONADA
    companion object {
        const val EXTRA_FECHA_SELECCIONADA = "extra_fecha_seleccionada"
    }
}