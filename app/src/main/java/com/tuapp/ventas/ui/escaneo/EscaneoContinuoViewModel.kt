package com.tuapp.ventas.ui.escaneo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.model.VentaItem
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.launch

class EscaneoContinuoViewModel(private val repo: VentasRepository) : ViewModel() {
    private val _productosAcumulados = MutableLiveData<List<VentaItem>>(emptyList())
    val productosAcumulados: LiveData<List<VentaItem>> = _productosAcumulados
    val cantidadProductos: LiveData<Int> = productosAcumulados.map { items -> items.sumOf { it.cantidad } }
    val productoEscaneado = MutableLiveData<Producto?>()
    val codigoNoEncontrado = MutableLiveData<String?>()
    val productoCreado = MutableLiveData<Producto?>()
    val mensaje = MutableLiveData<String>()

    fun consumirProductoEscaneado() { productoEscaneado.value = null }
    fun consumirCodigoNoEncontrado() { codigoNoEncontrado.value = null }
    fun consumirProductoCreado() { productoCreado.value = null }

    fun procesarCodigo(codigo: String) = viewModelScope.launch {
        val producto = repo.buscarProducto(codigo)
        if (producto == null) codigoNoEncontrado.value = codigo else productoEscaneado.value = producto
    }

    fun crearProductoEscaneado(codigo: String, nombre: String, precio: Double, inventario: Int) = viewModelScope.launch {
        runCatching { repo.crearProducto(codigo, nombre, precio, inventario) }
            .onSuccess { productoCreado.value = it }
            .onFailure { mensaje.value = it.message ?: "Error al guardar producto" }
    }

    fun productoConStockDisponible(producto: Producto): Producto {
        val yaAcumulado = _productosAcumulados.value.orEmpty()
            .firstOrNull { it.producto.id == producto.id }
            ?.cantidad ?: 0
        return producto.copy(vendidos = producto.vendidos + yaAcumulado)
    }

    fun agregarProducto(producto: Producto, cantidad: Int) {
        val stockDisponible = (producto.inventario - producto.vendidos).coerceAtLeast(0)
        if (stockDisponible <= 0) {
            mensaje.value = "Inventario insuficiente. Disponible: 0"
            return
        }
        val actual = _productosAcumulados.value.orEmpty().toMutableList()
        val index = actual.indexOfFirst { it.producto.id == producto.id }
        val cantidadActual = if (index >= 0) actual[index].cantidad else 0
        val nuevaCantidad = (cantidadActual + cantidad).coerceAtMost(stockDisponible.coerceAtMost(99))
        if (nuevaCantidad <= cantidadActual) {
            mensaje.value = "Inventario insuficiente. Disponible: ${(stockDisponible - cantidadActual).coerceAtLeast(0)}"
            return
        }
        if (index >= 0) actual[index] = actual[index].copy(cantidad = nuevaCantidad) else actual.add(VentaItem(producto, nuevaCantidad))
        _productosAcumulados.value = actual
    }

    fun obtenerProductosAcumulados(): ArrayList<VentaItem> = ArrayList(_productosAcumulados.value.orEmpty())
}

class EscaneoContinuoViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = EscaneoContinuoViewModel(repo) as T
}
