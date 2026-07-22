package com.tuapp.ventas.ui.venta

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.model.VentaItem
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VentaMultipleViewModel(private val repo: VentasRepository) : ViewModel() {
    private val ioJob = SupervisorJob()
    private val ioScope = CoroutineScope(ioJob + Dispatchers.IO)
    private val _productosAcumulados = MutableLiveData<List<VentaItem>>(emptyList())
    val productosAcumulados: LiveData<List<VentaItem>> = _productosAcumulados
    val mensaje = MutableLiveData<String>()
    val productoEscaneado = MutableLiveData<Producto?>()
    val codigoNoEncontrado = MutableLiveData<String?>()
    val productoCreado = MutableLiveData<Producto?>()

    fun consumirProductoEscaneado() { productoEscaneado.value = null }
    fun consumirCodigoNoEncontrado() { codigoNoEncontrado.value = null }
    fun consumirProductoCreado() { productoCreado.value = null }

    fun agregarProducto(producto: Producto, cantidad: Int) {
        Log.d(TAG, "agregarProducto: ${producto.nombre} x$cantidad")
        val stockDisponible = (producto.inventario - producto.vendidos).coerceAtLeast(0).coerceAtMost(99)
        if (stockDisponible <= 0) { mensaje.value = "Inventario insuficiente. Disponible: 0"; return }
        val actual = _productosAcumulados.value.orEmpty().toMutableList()
        val index = actual.indexOfFirst { it.producto.id == producto.id }
        val cantidadActual = if (index >= 0) actual[index].cantidad else 0
        val nuevaCantidad = (cantidadActual + cantidad).coerceAtMost(stockDisponible)
        if (nuevaCantidad <= cantidadActual) { mensaje.value = "Inventario insuficiente. Disponible: ${(stockDisponible - cantidadActual).coerceAtLeast(0)}"; return }
        if (index >= 0) actual[index] = actual[index].copy(cantidad = nuevaCantidad)
        else actual.add(VentaItem(producto, nuevaCantidad))
        _productosAcumulados.value = actual
    }

    fun agregarProductos(items: List<VentaItem>) {
        items.forEach { agregarProducto(it.producto, it.cantidad) }
    }

    fun eliminarProducto(item: VentaItem) {
        _productosAcumulados.value = _productosAcumulados.value.orEmpty().filterNot { it.producto.id == item.producto.id }
    }

    fun procesarEscaneo(codigo: String) = viewModelScope.launch {
        Log.d(TAG, "procesarEscaneo: $codigo")
        val producto = repo.buscarProducto(codigo)
        if (producto == null) codigoNoEncontrado.value = codigo else productoEscaneado.value = producto
    }

    fun buscarProductoPorId(id: Long) = viewModelScope.launch {
        repo.buscarProductoPorId(id)?.let { productoEscaneado.value = it } ?: run { mensaje.value = "Producto no encontrado" }
    }

    fun crearProductoEscaneado(codigo: String, nombre: String, precio: Double, inventario: Int) = viewModelScope.launch {
        runCatching { repo.crearProducto(codigo, nombre, precio, inventario) }
            .onSuccess { productoCreado.value = it }
            .onFailure { mensaje.value = it.message ?: "Error al guardar producto" }
    }

    fun registrarVenta(onResult: (String) -> Unit) {
        val items = _productosAcumulados.value.orEmpty()
        if (items.isEmpty()) { onResult("Agrega al menos un producto"); return }
        ioScope.launch {
            runCatching {
                withContext(NonCancellable) {
                    items.forEach { repo.registrarVentaDirecta(it.producto, it.cantidad) }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _productosAcumulados.value = emptyList()
                    onResult("Venta múltiple registrada")
                }
            }.onFailure {
                withContext(Dispatchers.Main) { onResult(it.message ?: "Error al registrar venta") }
            }
        }
    }

    override fun onCleared() { ioJob.cancel(); super.onCleared() }
    companion object { private const val TAG = "VentaMultipleVM" }
}

class VentaMultipleViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = VentaMultipleViewModel(repo) as T
}
