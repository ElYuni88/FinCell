package com.tuapp.ventas.ui.ipb

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.ProductoIPB
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.launch

class IPBResumenViewModel(private val repo: VentasRepository) : ViewModel() {
    private val _productosIPB = MutableLiveData<List<ProductoIPB>>(emptyList())
    val productosIPB: LiveData<List<ProductoIPB>> = _productosIPB

    fun cargarDatos() {
        viewModelScope.launch {
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
            _productosIPB.value = productos
        }
    }
}

class IPBResumenViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = IPBResumenViewModel(repo) as T
}