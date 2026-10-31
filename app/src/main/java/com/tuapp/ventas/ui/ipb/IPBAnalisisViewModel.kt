package com.tuapp.ventas.ui.ipb

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.GastoDesglose
import com.tuapp.ventas.data.model.ProductoAnalisis
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager
import kotlinx.coroutines.launch

class IPBAnalisisViewModel(
    private val repo: VentasRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _productos = MutableLiveData<List<ProductoAnalisis>>(emptyList())
    val productos: LiveData<List<ProductoAnalisis>> = _productos

    private val _gastos = MutableLiveData<List<GastoDesglose>>(emptyList())
    val gastos: LiveData<List<GastoDesglose>> = _gastos

    private val _totales = MutableLiveData<TotalesAnalisis>(TotalesAnalisis())
    val totales: LiveData<TotalesAnalisis> = _totales

    private val _rangoTexto = MutableLiveData<String>("")
    val rangoTexto: LiveData<String> = _rangoTexto

    fun cargar() = viewModelScope.launch {
        val prefs = preferencesManager
        val inicioPeriodo = prefs.obtenerInicioPeriodoVentas().takeIf { it > 0L }
            ?: DateUtils.inicioDia(System.currentTimeMillis())
        val inicioMuerto = prefs.ultimoInicioPeriodoMuerto.takeIf { it > 0L }
            ?: prefs.obtenerUltimaExportacionIPB().takeIf { it > 0L && it < inicioPeriodo }
            ?: 0L
        val fin = System.currentTimeMillis()

        val inicioParaDias = inicioMuerto.takeIf { it > 0L } ?: inicioPeriodo

        // Ventas del período muerto agrupadas por productoId
        val ventasMuerto = mutableMapOf<Long, Int>()
        if (inicioMuerto > 0L && inicioMuerto < inicioPeriodo) {
            repo.ventasDirectasDia(inicioMuerto, inicioPeriodo).forEach { v ->
                ventasMuerto[v.productoId] = (ventasMuerto[v.productoId] ?: 0) + 1
            }
            repo.cuentasCerradasDia(inicioMuerto, inicioPeriodo).forEach { c ->
                c.detalles.forEach { d ->
                    ventasMuerto[d.producto.id] = (ventasMuerto[d.producto.id] ?: 0) + d.detalle.cantidad
                }
            }
        }

        // Productos
        val productos = repo.listarProductos()
        val lista = productos.map { p ->
            val vendidosTotal = p.vendidos
            val vendidosMuerto = (ventasMuerto[p.id] ?: 0).coerceAtMost(vendidosTotal)
            val vendidosPeriodo = vendidosTotal - vendidosMuerto
            val entrada = (p.inventario - p.inventarioAlInicioPeriodo).coerceAtLeast(0)
            val stockFinal = p.inventario - vendidosTotal

            ProductoAnalisis(
                id = p.id,
                codigoBarras = p.codigoBarras,
                nombre = p.nombre,
                precio = p.precio,
                stockInicial = p.inventarioAlInicioPeriodo,
                entrada = entrada,
                vendidosTotal = vendidosTotal,
                vendidosMuerto = vendidosMuerto,
                vendidosPeriodo = vendidosPeriodo,
                stockFinal = stockFinal,
                subtotal = vendidosTotal * p.precio,
                inactivo = p.inactivo
            )
        }.sortedWith(
            compareByDescending<ProductoAnalisis> { it.vendidosTotal }
                .thenBy { it.nombre.lowercase() }
        )

        _productos.value = lista

        // Gastos del rango (incluye período muerto si aplica)
        val gastosRaw = prefs.obtenerGastosActivosRango(inicioParaDias, fin)
        val listaGastos = gastosRaw.map { GastoDesglose(nombre = it.categoria, monto = it.monto) }
            .sortedByDescending { it.monto }
        _gastos.value = listaGastos

        val totalVentas = lista.sumOf { it.subtotal }
        val totalGastos = listaGastos.sumOf { it.monto }

        _totales.value = TotalesAnalisis(
            totalVendidos = lista.sumOf { it.vendidosTotal },
            totalVentas = totalVentas,
            totalGastos = totalGastos,
            totalNeto = totalVentas - totalGastos
        )

        // Texto del rango
        val dias = ((fin - inicioParaDias) / (24 * 60 * 60 * 1000L) + 1).coerceAtLeast(1)
        val diasTexto = if (dias == 1L) "1 día" else "$dias días"
        val desdeStr = DateUtils.fechaHora(inicioParaDias)
        val incluyeMuerto = if (inicioMuerto > 0L && inicioMuerto < inicioPeriodo) " · Incluye período muerto" else ""
        _rangoTexto.value = "Desde $desdeStr · $diasTexto$incluyeMuerto"
    }

    data class TotalesAnalisis(
        val totalVendidos: Int = 0,
        val totalVentas: Double = 0.0,
        val totalGastos: Double = 0.0,
        val totalNeto: Double = 0.0
    )
}

class IPBAnalisisViewModelFactory(
    private val repo: VentasRepository,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        IPBAnalisisViewModel(repo, preferencesManager) as T
}