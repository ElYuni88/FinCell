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

    private val _totalVentas = MutableLiveData(0.0)
    val totalVentas: LiveData<Double> = _totalVentas

    private val _totalGastos = MutableLiveData(0.0)
    val totalGastos: LiveData<Double> = _totalGastos

    private val _totalNeto = MutableLiveData(0.0)
    val totalNeto: LiveData<Double> = _totalNeto

    /** Carga productos, ventas, cuentas cerradas y gastos guardados para la fecha indicada. */
    fun cargarDatos(fecha: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val inicio = DateUtils.inicioDia(fecha)
            val fin = DateUtils.finDia(fecha)

            // Obtener ventas directas y cuentas cerradas del día
            val ventasDirectas = repo.ventasDirectasDia(inicio, fin)
            val cuentasCerradas = repo.cuentasCerradasDia(inicio, fin)

            // Calcular totales
            val totalVentasDia = ventasDirectas.sumOf { it.precio } + cuentasCerradas.sumOf { it.cuenta.total }

            // Crear un mapa para acumular ventas por producto
            val ventasPorProducto = mutableMapOf<Long, ProductoIPB>()

            // Procesar ventas directas
            ventasDirectas.forEach { venta ->
                val producto = repo.buscarProductoPorId(venta.productoId) ?: return@forEach
                val key = producto.id
                val actual = ventasPorProducto[key]
                if (actual == null) {
                    ventasPorProducto[key] = ProductoIPB(
                        id = producto.id,
                        nombre = producto.nombre,
                        codigoBarras = producto.codigoBarras,
                        precio = producto.precio,
                        inventario = producto.inventario, // stock inicial
                        vendidos = 1
                    )
                } else {
                    ventasPorProducto[key] = actual.copy(vendidos = actual.vendidos + 1)
                }
            }

            // Procesar cuentas cerradas
            cuentasCerradas.forEach { cuenta ->
                cuenta.detalles.forEach { detalleConProducto ->
                    val producto = detalleConProducto.producto
                    val detalle = detalleConProducto.detalle
                    val key = producto.id
                    val actual = ventasPorProducto[key]
                    if (actual == null) {
                        ventasPorProducto[key] = ProductoIPB(
                            id = producto.id,
                            nombre = producto.nombre,
                            codigoBarras = producto.codigoBarras,
                            precio = producto.precio,
                            inventario = producto.inventario,
                            vendidos = detalle.cantidad
                        )
                    } else {
                        ventasPorProducto[key] = actual.copy(vendidos = actual.vendidos + detalle.cantidad)
                    }
                }
            }

            // Obtener gastos del día
            val fechaStr = DateUtils.fechaArchivo(fecha)
            val gastosDia = preferencesManager.obtenerGastos(fechaStr)
           // val gastosDia = preferencesManager.obtenerGastos()
            val totalGastosDia = gastosDia.sumOf { it.monto }

            // Actualizar LiveData
            _productosIPB.value = ventasPorProducto.values.sortedBy { it.nombre }
            _gastos.value = gastosDia
            _totalVentas.value = totalVentasDia
            _totalGastos.value = totalGastosDia
            _totalNeto.value = totalVentasDia - totalGastosDia
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
