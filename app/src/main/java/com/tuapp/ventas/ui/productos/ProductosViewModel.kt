package com.tuapp.ventas.ui.productos

import androidx.lifecycle.MutableLiveData
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.launch

/** ViewModel de administración CRUD de productos. */
class ProductosViewModel(private val repo: VentasRepository) : ViewModel() {
    val productos = repo.obtenerTodosLosProductos().asLiveData()
    val mensaje = MutableLiveData<String>()

    suspend fun verificarCodigoDuplicado(codigo: String): Boolean = repo.verificarCodigoDuplicado(codigo)

    fun guardar(producto: Producto) = viewModelScope.launch {
        runCatching {
            val codigo = producto.codigoBarras.trim()
            if (codigo.isNotBlank() && repo.existeCodigoDuplicado(codigo, producto.id)) {
                error("Ya existe un producto con ese código")
            }
            val normalizado = producto.copy(codigoBarras = codigo)
            Log.d(TAG, "Guardando producto: esManual=${normalizado.esManual}, tipo=${normalizado.tipoProducto}, codigo=${normalizado.codigoBarras}")
            repo.guardarProducto(normalizado)
        }.onSuccess {
            mensaje.value = "Producto guardado"
        }.onFailure {
            mensaje.value = it.message ?: "No se pudo guardar el producto"
        }
    }

    fun eliminar(producto: Producto) = viewModelScope.launch {
        runCatching { repo.eliminarProducto(producto) }
            .onSuccess { mensaje.value = "Producto eliminado" }
            .onFailure { mensaje.value = it.message ?: "No se pudo eliminar el producto" }
    }

    companion object {
        private const val TAG = "ProductosViewModel"
    }

    fun generarCodigoManualSugerido(): String {
        val siguiente = ((productos.value?.maxOfOrNull { it.id } ?: 0L) + 1L).coerceAtLeast(1L)
        return "MANUAL_%03d".format(siguiente)
    }
}

class ProductosViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ProductosViewModel(repo) as T
}
