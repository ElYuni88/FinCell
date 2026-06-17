package com.tuapp.ventas.ui.productos

import androidx.lifecycle.*
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.launch

class ProductosViewModel(private val repo: VentasRepository): ViewModel(){
 val productos = repo.observarProductos().asLiveData()
 val mensaje = MutableLiveData<String>()
 fun guardar(producto: Producto)=viewModelScope.launch{ runCatching{ repo.guardarProducto(producto) }.onSuccess{ mensaje.value="Producto guardado" }.onFailure{ mensaje.value=it.message } }
 fun eliminar(producto: Producto)=viewModelScope.launch{ runCatching{ repo.eliminarProducto(producto) }.onSuccess{ mensaje.value="Producto eliminado" }.onFailure{ mensaje.value=it.message } }
}
class ProductosViewModelFactory(private val repo: VentasRepository): ViewModelProvider.Factory{ @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>)=ProductosViewModel(repo) as T }
