package com.tuapp.ventas.ui.cuenta

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.VentaFinal
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.launch

class AccountDetailViewModel(private val repo: VentasRepository, private val cuentaId: Long) : ViewModel() {
    val cuenta = repo.observarCuenta(cuentaId).asLiveData()
    private val _ventaFinal = MutableLiveData<VentaFinal?>()
    val ventaFinal: LiveData<VentaFinal?> = _ventaFinal
    val mensaje = MutableLiveData<String>()
    val codigoProductoNuevo = MutableLiveData<String?>()

    init { cargarVentaFinal() }

    fun procesarEscaneoDirecto(codigo: String) = viewModelScope.launch {
        try {
            Log.d(TAG, "Buscando producto con código: $codigo")
            val producto = repo.buscarProducto(codigo)
            if (producto == null) {
                codigoProductoNuevo.value = codigo
                return@launch
            }

            repo.agregarProductoACuenta(cuentaId, producto, 1)
            Log.d(TAG, "${producto.nombre} agregado a la cuenta $cuentaId")
            mensaje.value = "${producto.nombre} agregado a la cuenta"
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar escaneo directo", e)
            mensaje.value = "Error: ${e.message ?: "No se pudo agregar"}"
        }
    }

    fun crearProductoYAgregar(codigo: String, nombre: String, precio: Double, cantidad: Int = 1) = viewModelScope.launch {
        try {
            val producto = repo.crearProducto(codigo, nombre, precio)
            repo.agregarProductoACuenta(cuentaId, producto, cantidad)
            mensaje.value = "${producto.nombre} creado y agregado (x$cantidad)"
        } catch (e: Exception) {
            mensaje.value = e.message ?: "No se pudo agregar"
        }
    }

    fun cambiarCantidad(detalleId: Long, nuevaCantidad: Int) = viewModelScope.launch {
        runCatching { repo.actualizarCantidadDetalle(detalleId, nuevaCantidad) }
            .onFailure { mensaje.value = it.message ?: "No se pudo actualizar la cantidad" }
    }

    fun eliminarDetalle(detalleId: Long) = viewModelScope.launch {
        runCatching { repo.eliminarDetalle(detalleId) }
            .onSuccess { mensaje.value = "Producto eliminado" }
            .onFailure { mensaje.value = it.message ?: "No se pudo eliminar" }
    }

    fun cerrarCuenta(metodoPago: String, observaciones: String?) = viewModelScope.launch {
        runCatching { repo.cerrarCuenta(cuentaId, metodoPago, observaciones) }
            .onSuccess { mensaje.value = "Cuenta cerrada correctamente"; cargarVentaFinal() }
            .onFailure { mensaje.value = it.message ?: "No se pudo cerrar la cuenta" }
    }

    fun cargarVentaFinal() = viewModelScope.launch { _ventaFinal.value = repo.obtenerVentaFinal(cuentaId) }

    companion object { private const val TAG = "AccountDetailVM" }
}

class AccountDetailViewModelFactory(private val repo: VentasRepository, private val cuentaId: Long) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountDetailViewModel(repo, cuentaId) as T
}
