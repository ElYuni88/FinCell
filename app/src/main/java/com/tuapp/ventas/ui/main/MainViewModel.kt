package com.tuapp.ventas.ui.main

import androidx.lifecycle.*
import com.tuapp.ventas.data.model.*
import com.tuapp.ventas.data.model.relaciones.CuentaConDetalles
import com.tuapp.ventas.data.model.relaciones.CuentaResumen
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar
import kotlinx.coroutines.launch

class MainViewModel(private val repo: VentasRepository) : ViewModel() {
    private val fechaSeleccionadaFlow = MutableStateFlow(inicioDia(System.currentTimeMillis()))
    val fechaSeleccionada: LiveData<Long> = fechaSeleccionadaFlow.asLiveData()

    // ✅ NUEVO: filtro de cuentas cerradas
    private val mostrarCerradasFlow = MutableStateFlow(false)

    val ventasHoy = fechaSeleccionadaFlow.flatMapLatest { repo.ventasDirectasPorFecha(it) }.asLiveData()
    val totalSimple = fechaSeleccionadaFlow.flatMapLatest { repo.totalVentasDirectasPorFecha(it) }.asLiveData()
    val cantidadSimple = fechaSeleccionadaFlow.flatMapLatest { repo.cantidadVentasDirectasPorFecha(it) }.asLiveData()

    /**
     * ✅ Cuentas activas según el filtro del Switch.
     * - Switch OFF (default): solo cuentas ABIERTAS del día
     * - Switch ON: cuentas ABIERTAS + CERRADAS del día
     *   (abiertas primero, luego cerradas por fecha de cierre DESC)
     */
    val cuentasActivas: LiveData<List<CuentaResumen>> =
        combine(fechaSeleccionadaFlow, mostrarCerradasFlow) { fecha, mostrarCerradas ->
            fecha to mostrarCerradas
        }.flatMapLatest { (fecha, mostrarCerradas) ->
            if (mostrarCerradas) {
                combine(
                    repo.resumenCuentasAbiertasPorFecha(fecha),
                    repo.resumenCuentasCerradasPorFecha(fecha)
                ) { abiertas, cerradas -> abiertas + cerradas }
            } else {
                repo.resumenCuentasAbiertasPorFecha(fecha)
            }
        }.asLiveData()

    val cantidadCuentasAbiertas = repo.cantidadCuentasAbiertas().asLiveData()
    private val cuentaSeleccionada = MutableStateFlow(-1L)
    val cuentaActual: LiveData<CuentaConDetalles?> = cuentaSeleccionada.flatMapLatest { if (it > 0) repo.observarCuenta(it) else flowOf(null) }.asLiveData()
    val mensaje = MutableLiveData<String>()
    val productoEscaneado = MutableLiveData<Producto?>()
    val codigoNuevoProducto = MutableLiveData<String?>()
    val productoCreadoParaVenta = MutableLiveData<Producto?>()

    fun seleccionarCuenta(id: Long) { cuentaSeleccionada.value = id }
    fun seleccionarFecha(fecha: Long) { fechaSeleccionadaFlow.value = inicioDia(fecha) }

    /** ✅ NUEVO: cambiar el filtro de cuentas cerradas */
    fun setMostrarCerradas(mostrar: Boolean) {
        mostrarCerradasFlow.value = mostrar
    }

    fun esFechaActual(): Boolean = inicioDia(fechaSeleccionadaFlow.value) == inicioDia(System.currentTimeMillis())
    fun generarNotificaciones() = viewModelScope.launch { repo.generarNotificacionesSistema() }
    suspend fun verificarCodigoDuplicado(codigo: String): Boolean = repo.verificarCodigoDuplicado(codigo)
    fun procesarEscaneo(codigoBarras: String, modo: ModoOperacion) = viewModelScope.launch {
        val producto = repo.buscarProducto(codigoBarras)
        if (modo == ModoOperacion.CUENTA && cuentaSeleccionada.value <= 0) { mensaje.value = "Primero selecciona o crea una cuenta"; return@launch }
        if (producto == null) codigoNuevoProducto.value = codigoBarras else productoEscaneado.value = producto
    }

    fun procesarEscaneoAlta(codigoBarras: String) = viewModelScope.launch {
        val producto = repo.buscarProducto(codigoBarras)
        if (producto == null)
            codigoNuevoProducto.value = "FAB:$codigoBarras"
        else mensaje.value = "Producto ya registrado"
    }
    fun registrarVentaDirecta(producto: Producto, cantidad: Int = 1) = viewModelScope.launch {
        val cantidadFinal = cantidad.coerceIn(1, 99)
        runCatching { repo.registrarVentaDirecta(producto, cantidadFinal) }
            .onSuccess { mensaje.value = "Venta registrada: ${producto.nombre} x$cantidadFinal" }
            .onFailure { mensaje.value = it.message ?: "Error al registrar" }
    }

    fun crearProductoEscaneado(codigo: String, nombre: String, precio: Double, inventario: Int = 0, abrirVenta: Boolean = false) = viewModelScope.launch {
        runCatching { repo.crearProducto(codigo, nombre, precio, inventario) }
            .onSuccess { producto ->
                if (abrirVenta) productoCreadoParaVenta.value = producto else mensaje.value = "Producto guardado exitosamente"
            }
            .onFailure { mensaje.value = it.message ?: "Error al guardar producto" }
    }

    fun crearProductoYVender(codigo: String, nombre: String, precio: Double, modo: ModoOperacion, cantidad: Int = 1, inventario: Int = 0) = viewModelScope.launch {
        val cantidadFinal = cantidad.coerceIn(1, 99)
        runCatching {
            val producto = repo.crearProducto(codigo, nombre, precio, inventario)
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
        // ✅ NUEVO: verificar si el cliente ya existe en BD
        val nombreTrim = nombre.trim()
        val clienteExistente = if (nombreTrim.isNotBlank()) {
            repo.buscarClientesPorNombre(nombreTrim)
                .firstOrNull { it.nombre.equals(nombreTrim, ignoreCase = true) }
        } else null

        // Regla:
        // - Si el cliente ya existe en BD → SIEMPRE asociarlo (cuenta NO temporal)
        // - Si no existe → respetar el checkbox del usuario
        val recordarFinal = clienteExistente != null || recordarCuenta

        val id = if (recordarFinal) {
            repo.crearCuenta(nombre, telefono, mesa, recordarCuenta = true)
        } else {
            repo.crearCuentaTemporal(nombre, mesa)
        }
        seleccionarCuenta(id)

        mensaje.value = when {
            clienteExistente != null -> "Cuenta creada para cliente existente"
            recordarCuenta -> "Cuenta creada y cliente guardado"
            else -> "Cuenta temporal creada"
        }
    }
    fun eliminarCuentaVacia(cuentaId: Long) = viewModelScope.launch {
        runCatching { repo.eliminarCuentaVacia(cuentaId) }
            .onSuccess {
                if (cuentaSeleccionada.value == cuentaId) seleccionarCuenta(-1L)
                mensaje.value = "Cuenta eliminada"
            }
            .onFailure { mensaje.value = it.message ?: "No se pudo eliminar la cuenta" }
    }
    fun registrarProductoManual(producto: Producto, cantidad: Int, modo: ModoOperacion) = viewModelScope.launch {
        val cantidadFinal = cantidad.coerceIn(1, 99)
        runCatching {
            val guardado = if (producto.id == 0L) repo.crearProductoManual(producto.codigoBarras, producto.nombre, producto.precio) else producto
            if (modo == ModoOperacion.SIMPLE) repo.registrarVentaDirecta(guardado, cantidadFinal) else repo.agregarProductoACuenta(cuentaSeleccionada.value, guardado, cantidadFinal)
            guardado
        }.onSuccess { mensaje.value = "${it.nombre} agregado x$cantidadFinal" }
            .onFailure { mensaje.value = it.message ?: "No se pudo agregar el producto manual" }
    }

    fun agregarACuenta(producto: Producto, cantidad: Int) = viewModelScope.launch {
        val id = cuentaSeleccionada.value
        if (id <= 0) mensaje.value = "Primero selecciona o crea una cuenta" else runCatching { repo.agregarProductoACuenta(id, producto, cantidad) }.onSuccess { mensaje.value = "${producto.nombre} agregado a la cuenta" }.onFailure { mensaje.value = it.message ?: "No se pudo agregar a la cuenta" }
    }

    companion object {
        private fun inicioDia(fecha: Long): Long = Calendar.getInstance().apply { timeInMillis = fecha; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    }
}
class MainViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
}