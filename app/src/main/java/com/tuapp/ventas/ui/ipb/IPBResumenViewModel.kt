package com.tuapp.ventas.ui.ipb

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.Gasto
import com.tuapp.ventas.data.model.ProductoIPB
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.utils.DateUtils
import com.tuapp.ventas.utils.PreferencesManager
import kotlinx.coroutines.launch

class IPBResumenViewModel(
    private val repo: VentasRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _productosIPB = MutableLiveData<List<ProductoIPB>>(emptyList())
    val productosIPB: LiveData<List<ProductoIPB>> = _productosIPB

    private val _gastos = MutableLiveData<List<Gasto>>(emptyList())
    val gastos: LiveData<List<Gasto>> = _gastos

    private val _ingresos = MutableLiveData<List<Gasto>>(emptyList())
    val ingresos: LiveData<List<Gasto>> = _ingresos

    private val _totalVentas = MutableLiveData(0.0)
    val totalVentas: LiveData<Double> = _totalVentas

    private val _totalGastos = MutableLiveData(0.0)
    val totalGastos: LiveData<Double> = _totalGastos

    private val _totalIngresos = MutableLiveData(0.0)
    val totalIngresos: LiveData<Double> = _totalIngresos

    private val _totalNeto = MutableLiveData(0.0)
    val totalNeto: LiveData<Double> = _totalNeto

    private val _cantidadVentas = MutableLiveData(0)
    val cantidadVentas: LiveData<Int> = _cantidadVentas

    private val _cantidadCuentas = MutableLiveData(0)
    val cantidadCuentas: LiveData<Int> = _cantidadCuentas

    /**
     * Carga los datos del PERIODO completo (desde inicioPeriodoVentas hasta ahora).
     * El parámetro `fecha` se mantiene por compatibilidad con llamadas viejas, pero
     * se ignora: siempre se usa el rango del periodo abierto.
     */
    fun cargarDatos(@Suppress("UNUSED_PARAMETER") fecha: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val inicio = preferencesManager.obtenerInicioPeriodoVentas().takeIf { it > 0L }
                ?: DateUtils.inicioDia(System.currentTimeMillis())
            val fin = System.currentTimeMillis()

            val ventasDirectas = repo.ventasDirectasDia(inicio, fin)
            val cuentasCerradas = repo.cuentasCerradasDia(inicio, fin)

            val totalVentasDirectas = ventasDirectas.sumOf { it.precio }
            val totalCuentasCerradas = cuentasCerradas.sumOf { it.cuenta.total }
            val totalVentasDia = totalVentasDirectas + totalCuentasCerradas

            val ventasPorProducto = mutableMapOf<Long, ProductoIPB>()

            ventasDirectas.forEach { venta ->
                val producto = repo.buscarProductoPorId(venta.productoId) ?: return@forEach
                val actual = ventasPorProducto[producto.id]
                ventasPorProducto[producto.id] = if (actual == null) {
                    ProductoIPB(
                        id = producto.id,
                        nombre = producto.nombre,
                        codigoBarras = producto.codigoBarras,
                        precio = producto.precio,
                        inventario = producto.inventario,
                        vendidos = 1
                    )
                } else {
                    actual.copy(vendidos = actual.vendidos + 1)
                }
            }

            cuentasCerradas.forEach { cuenta ->
                cuenta.detalles.forEach { detalleConProducto ->
                    val producto = detalleConProducto.producto
                    val detalle = detalleConProducto.detalle
                    val actual = ventasPorProducto[producto.id]
                    ventasPorProducto[producto.id] = if (actual == null) {
                        ProductoIPB(
                            id = producto.id,
                            nombre = producto.nombre,
                            codigoBarras = producto.codigoBarras,
                            precio = producto.precio,
                            inventario = producto.inventario,
                            vendidos = detalle.cantidad
                        )
                    } else {
                        actual.copy(vendidos = actual.vendidos + detalle.cantidad)
                    }
                }
            }

            // ✅ Gastos e ingresos de TODO el rango del periodo
            val gastosDia = preferencesManager.obtenerGastosActivosRango(inicio, fin)
            val ingresosDia = preferencesManager.obtenerIngresosActivosRango(inicio, fin)

            val totalGastosDia = gastosDia.sumOf { it.monto }
            val totalIngresosDia = ingresosDia.sumOf { it.monto }

            _productosIPB.value = ventasPorProducto.values.sortedBy { it.nombre }
            _gastos.value = gastosDia
            _ingresos.value = ingresosDia

            _totalVentas.value = totalVentasDia
            _totalGastos.value = totalGastosDia
            _totalIngresos.value = totalIngresosDia
            _cantidadVentas.value = ventasDirectas.size
            _cantidadCuentas.value = cuentasCerradas.size
            _totalNeto.value = (totalVentasDia + totalIngresosDia) - totalGastosDia
        }
    }
}

class IPBResumenViewModelFactory(
    private val repo: VentasRepository,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        IPBResumenViewModel(repo, preferencesManager) as T
}