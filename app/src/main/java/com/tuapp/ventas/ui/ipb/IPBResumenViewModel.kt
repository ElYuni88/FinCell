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
            val productos = repo.listarProductos().map {
                ProductoIPB(
                    id = it.id,
                    nombre = it.nombre,
                    codigoBarras = it.codigoBarras,
                    precio = it.precio,
                    inventario = it.inventario,
                    vendidos = it.vendidos
                )
            }
            val ventasDirectas = repo.ventasDirectasDia(inicio, fin).sumOf { it.precio }
            val ventasCuentas = repo.cuentasCerradasDia(inicio, fin).sumOf { it.cuenta.total }
            val gastosDia = preferencesManager.obtenerGastos(DateUtils.fechaArchivo(fecha))
            val totalVentasDia = ventasDirectas + ventasCuentas
            val totalGastosDia = gastosDia.sumOf { it.monto }

            _productosIPB.value = productos
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
