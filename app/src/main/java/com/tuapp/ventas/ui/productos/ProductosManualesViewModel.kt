package com.tuapp.ventas.ui.productosmanuales

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.model.VentaItem
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.utils.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modelo para la card de producto con stock ajustado según el carrito.
 *
 * - producto: el producto original de la BD
 * - cantidadEnCarrito: cuántas unidades del producto ya están en el carrito
 * - stockDisponible: inventario - vendidos - cantidadEnCarrito (nunca negativo)
 * - sinStock: true si stockDisponible <= 0 (o si inventario - vendidos ya era 0)
 */
data class ProductoConStockAjustado(
    val producto: Producto,
    val cantidadEnCarrito: Int
) {
    val stockDisponible: Int
        get() = (producto.inventario - producto.vendidos - cantidadEnCarrito).coerceAtLeast(0)

    val sinStock: Boolean
        get() = stockDisponible <= 0
}

class ProductosManualesViewModel(private val repo: VentasRepository) : ViewModel() {

    // ============================================================
    // PRODUCTOS
    // ============================================================
    // ✅ Solo productos activos (los inactivos no aparecen en venta manual)
    private val todosLosProductos = repo.observarProductosActivos().asLiveData()
    private val query = MutableLiveData("")

    // ============================================================
    // CARRITO TEMPORAL
    // ============================================================
    private val ioJob = SupervisorJob()
    private val ioScope = CoroutineScope(ioJob + Dispatchers.IO)

    private val _productosAcumulados = MutableLiveData<List<VentaItem>>(emptyList())
    val productosAcumulados: LiveData<List<VentaItem>> = _productosAcumulados

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> = _mensaje

    private val _ventaRegistrada = MutableLiveData<Boolean>()
    val ventaRegistrada: LiveData<Boolean> = _ventaRegistrada

    // ============================================================
    // PRODUCTOS CON STOCK AJUSTADO (productos + carrito combinados)
    // ============================================================
    /**
     * Lista de productos con el stock ajustado según lo que ya está en el carrito.
     * Se recalcula automáticamente cuando cambia:
     * - La lista de productos (por ejemplo, si cambia el inventario en BD)
     * - La búsqueda
     * - El carrito (agregar/quitar productos)
     */
    val productosConStockAjustado: LiveData<List<ProductoConStockAjustado>> = combine(
        todosLosProductos.asFlow(),
        query.asFlow(),
        _productosAcumulados.asFlow()
    ) { productos, q, carrito ->
        // 1. Filtrar por búsqueda
        val filtrados = if (q.isBlank()) {
            productos
        } else {
            productos.filter { producto ->
                TextUtils.contiene(producto.nombre, q) ||
                        producto.codigoBarras.contains(q, ignoreCase = true)
            }
        }

        // 2. Mapear con cantidad en carrito
        val carritoPorId = carrito.associateBy { it.producto.id }
        filtrados.map { p ->
            val itemEnCarrito = carritoPorId[p.id]
            ProductoConStockAjustado(
                producto = p,
                cantidadEnCarrito = itemEnCarrito?.cantidad ?: 0
            )
        }
    }.asLiveData()

    fun setQuery(q: String) {
        query.value = q
    }

    // ============================================================
    // ACCIONES DEL CARRITO
    // ============================================================

    /**
     * Agrega un producto al carrito temporal.
     * Si ya existe, suma la cantidad respetando el stock.
     */
    fun agregarAlCarrito(producto: Producto, cantidad: Int) {
        Log.d(TAG, "agregarAlCarrito: ${producto.nombre} x$cantidad")
        val stockDisponible = (producto.inventario - producto.vendidos).coerceAtLeast(0).coerceAtMost(99)

        if (stockDisponible <= 0) {
            _mensaje.value = "Inventario insuficiente. Disponible: 0"
            return
        }

        val actual = _productosAcumulados.value.orEmpty().toMutableList()
        val index = actual.indexOfFirst { it.producto.id == producto.id }
        val cantidadActual = if (index >= 0) actual[index].cantidad else 0
        val nuevaCantidad = (cantidadActual + cantidad).coerceAtMost(stockDisponible)

        if (nuevaCantidad <= cantidadActual) {
            _mensaje.value = "Inventario insuficiente. Disponible: ${(stockDisponible - cantidadActual).coerceAtLeast(0)}"
            return
        }

        if (index >= 0) actual[index] = actual[index].copy(cantidad = nuevaCantidad)
        else actual.add(VentaItem(producto, nuevaCantidad))

        _productosAcumulados.value = actual
    }

    /**
     * Elimina un producto del carrito.
     */
    fun eliminarDelCarrito(item: VentaItem) {
        _productosAcumulados.value = _productosAcumulados.value.orEmpty().filterNot { it.producto.id == item.producto.id }
    }

    /**
     * Limpia el carrito.
     */
    fun limpiarCarrito() {
        _productosAcumulados.value = emptyList()
    }

    /**
     * Registra todos los productos del carrito.
     * @param modo "SIMPLE" o "CUENTA"
     * @param cuentaId ID de la cuenta (si aplica)
     */
    fun registrarVenta(modo: String, cuentaId: Long) {
        val items = _productosAcumulados.value.orEmpty()
        if (items.isEmpty()) {
            _mensaje.value = "Agrega al menos un producto"
            return
        }

        ioScope.launch {
            runCatching {
                withContext(NonCancellable) {
                    items.forEach { item ->
                        if (modo == "SIMPLE") {
                            repo.registrarVentaDirecta(item.producto, item.cantidad)
                        } else {
                            repo.agregarProductoACuenta(cuentaId, item.producto, item.cantidad)
                        }
                    }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _productosAcumulados.value = emptyList()
                    _mensaje.value = "Venta registrada: ${items.size} productos"
                    _ventaRegistrada.value = true
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    _mensaje.value = it.message ?: "Error al registrar venta"
                }
            }
        }
    }

    fun consumirVentaRegistrada() {
        _ventaRegistrada.value = false
    }

    override fun onCleared() {
        ioJob.cancel()
        super.onCleared()
    }

    companion object {
        private const val TAG = "ProductosManualesVM"
    }
}

class ProductosManualesViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ProductosManualesViewModel(repo) as T
}