package com.tuapp.ventas.ui.main

import androidx.lifecycle.*
import com.tuapp.ventas.data.model.*
import com.tuapp.ventas.data.model.relaciones.CuentaConDetalles
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainViewModel(private val repo: VentasRepository) : ViewModel() {
    val ventasHoy = repo.ventasDirectasHoy().asLiveData()
    val totalSimple = repo.totalVentasDirectasHoy().asLiveData()
    val cantidadSimple = repo.cantidadVentasDirectasHoy().asLiveData()
    val cuentasActivas = repo.resumenCuentas().asLiveData()
    val cantidadCuentasAbiertas = repo.cantidadCuentasAbiertas().asLiveData()
    private val cuentaSeleccionada = MutableStateFlow(-1L)
    val cuentaActual: LiveData<CuentaConDetalles?> = cuentaSeleccionada.flatMapLatest { if (it > 0) repo.observarCuenta(it) else flowOf(null) }.asLiveData()
    val mensaje = MutableLiveData<String>()
    val productoEscaneado = MutableLiveData<Producto?>()
    val codigoNuevoProducto = MutableLiveData<String?>()

    fun seleccionarCuenta(id: Long) { cuentaSeleccionada.value = id }
    fun procesarEscaneo(codigoBarras: String, modo: ModoOperacion) = viewModelScope.launch {
        val producto = repo.buscarProducto(codigoBarras)
        if (modo == ModoOperacion.CUENTA && cuentaSeleccionada.value <= 0) { mensaje.value = "Primero selecciona o crea una cuenta"; return@launch }
        if (producto == null) codigoNuevoProducto.value = codigoBarras else productoEscaneado.value = producto
    }
    fun registrarVentaDirecta(producto: Producto, cantidad: Int = 1) = viewModelScope.launch {
        val cantidadFinal = cantidad.coerceIn(1, 99)
        runCatching { repo.registrarVentaDirecta(producto, cantidadFinal) }
            .onSuccess { mensaje.value = "Venta registrada: ${producto.nombre} x$cantidadFinal" }
            .onFailure { mensaje.value = it.message ?: "Error al registrar" }
    }
    fun crearProductoYVender(codigo: String, nombre: String, precio: Double, modo: ModoOperacion, cantidad: Int = 1) = viewModelScope.launch {
        val cantidadFinal = cantidad.coerceIn(1, 99)
        runCatching {
            val producto = repo.crearProducto(codigo, nombre, precio)
            if (modo == ModoOperacion.SIMPLE) {
                repo.registrarVentaDirecta(producto, cantidadFinal)
                "Venta registrada: ${producto.nombre} x$cantidadFinal"
            } else {
                repo.agregarProductoACuenta(cuentaSeleccionada.value, producto, cantidadFinal)
                "${producto.nombre} agregado a la cuenta"
            }
        }.onSuccess { mensaje.value = it }
            .onFailure { mensaje.value = it.message ?: "Error al registrar" }
    }
    fun crearCuenta(nombre: String, telefono: String?, mesa: String?, recordarCuenta: Boolean) = viewModelScope.launch {
        val id = if (recordarCuenta) repo.crearCuenta(nombre, telefono, mesa, recordarCuenta = true) else repo.crearCuentaTemporal(nombre, mesa)
        seleccionarCuenta(id)
        mensaje.value = if (recordarCuenta) "Cuenta creada y cliente guardado" else "Cuenta temporal creada"
    }
    fun eliminarCuentaVacia(cuentaId: Long) = viewModelScope.launch {
        runCatching { repo.eliminarCuentaVacia(cuentaId) }
            .onSuccess {
                if (cuentaSeleccionada.value == cuentaId) seleccionarCuenta(-1L)
                mensaje.value = "Cuenta eliminada"
            }
            .onFailure { mensaje.value = it.message ?: "No se pudo eliminar la cuenta" }
    }
    fun agregarACuenta(producto: Producto, cantidad: Int) = viewModelScope.launch {
        val id = cuentaSeleccionada.value
        if (id <= 0) mensaje.value = "Primero selecciona o crea una cuenta" else runCatching { repo.agregarProductoACuenta(id, producto, cantidad) }.onSuccess { mensaje.value = "${producto.nombre} agregado a la cuenta" }.onFailure { mensaje.value = it.message ?: "No se pudo agregar a la cuenta" }
    }
}
class MainViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
}
