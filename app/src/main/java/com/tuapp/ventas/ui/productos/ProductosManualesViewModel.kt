package com.tuapp.ventas.ui.productosmanuales

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProductosManualesViewModel(private val repo: VentasRepository) : ViewModel() {
    // ✅ Ahora todos los productos (sin filtrar por esManual)
    private val todosLosProductos = repo.obtenerTodosLosProductos().asLiveData()

    private val query = MutableLiveData("")

    // Productos filtrados por búsqueda (sobre todos los productos)
    val productosFiltrados: LiveData<List<Producto>> = combine(
        todosLosProductos.asFlow(),
        query.asFlow()
    ) { productos, q ->
        if (q.isBlank()) productos
        else productos.filter { it.nombre.contains(q, ignoreCase = true) }
    }.asLiveData()

    fun setQuery(q: String) {
        query.value = q
    }
}

class ProductosManualesViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ProductosManualesViewModel(repo) as T
}