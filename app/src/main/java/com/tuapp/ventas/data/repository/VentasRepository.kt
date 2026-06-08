package com.tuapp.ventas.data.repository

import com.tuapp.ventas.data.database.AppDatabase
import com.tuapp.ventas.data.model.*
import com.tuapp.ventas.data.model.relaciones.CuentaConDetalles
import com.tuapp.ventas.utils.DateUtils
import kotlinx.coroutines.flow.Flow

class VentasRepository(private val db: AppDatabase) {
    private val productos = db.productoDao()
    private val ventas = db.ventaDirectaDao()
    private val clientes = db.clienteDao()
    private val cuentas = db.cuentaDao()
    private val detalles = db.detalleCuentaDao()
    private val finales = db.ventaFinalDao()

    fun ventasDirectasHoy(): Flow<List<VentaDirecta>> = ventas.observarDelDia(DateUtils.inicioDia(), DateUtils.finDia())
    fun totalVentasDirectasHoy(): Flow<Double> = ventas.observarTotalDia(DateUtils.inicioDia(), DateUtils.finDia())
    fun cantidadVentasDirectasHoy(): Flow<Int> = ventas.observarCantidadDia(DateUtils.inicioDia(), DateUtils.finDia())
    fun cuentasActivas(): Flow<List<Cuenta>> = cuentas.observarAbiertas()
    fun totalCuentasCerradasHoy(): Flow<Double> = cuentas.observarTotalCerradasDia(DateUtils.inicioDia(), DateUtils.finDia())
    fun cantidadCuentasCerradasHoy(): Flow<Int> = cuentas.observarCantidadCerradasDia(DateUtils.inicioDia(), DateUtils.finDia())
    fun observarCuenta(cuentaId: Long): Flow<CuentaConDetalles?> = cuentas.observarCuentaConDetalles(cuentaId)

    suspend fun buscarProducto(codigo: String): Producto? = productos.buscarPorCodigo(codigo)
    suspend fun crearProducto(codigo: String, nombre: String, precio: Double): Producto {
        val id = productos.insertar(Producto(codigoBarras = codigo, nombre = nombre, precio = precio))
        return productos.buscarPorCodigo(codigo)!!.copy(id = id)
    }
    suspend fun registrarVentaDirecta(producto: Producto): Long = ventas.insertar(VentaDirecta(productoId = producto.id, codigoBarras = producto.codigoBarras, nombreProducto = producto.nombre, precio = producto.precio))
    suspend fun crearCuenta(nombreCliente: String, telefono: String?): Long {
        val clienteId = clientes.insertar(Cliente(nombre = nombreCliente, telefono = telefono?.ifBlank { null }))
        return cuentas.insertar(Cuenta(clienteId = clienteId))
    }
    suspend fun agregarProductoACuenta(cuentaId: Long, producto: Producto, cantidad: Int) {
        val subtotal = producto.precio * cantidad
        detalles.insertar(DetalleCuenta(cuentaId = cuentaId, productoId = producto.id, cantidad = cantidad, precioUnitario = producto.precio, subtotal = subtotal))
        cuentas.actualizarTotal(cuentaId, detalles.totalCuenta(cuentaId))
    }
    suspend fun cerrarCuenta(cuentaId: Long, metodoPago: String, observaciones: String?) {
        val cuenta = cuentas.obtener(cuentaId) ?: error("Cuenta no encontrada")
        val total = detalles.totalCuenta(cuentaId)
        val cierre = System.currentTimeMillis()
        cuentas.actualizar(cuenta.copy(estado = Cuenta.ESTADO_CERRADA, fechaCierre = cierre, total = total))
        finales.insertar(VentaFinal(cuentaId = cuentaId, totalVenta = total, metodoPago = metodoPago, fechaCierre = cierre, observaciones = observaciones))
    }
    suspend fun obtenerCuentaConDetalles(cuentaId: Long): CuentaConDetalles? = cuentas.obtenerCuentaConDetalles(cuentaId)
    suspend fun ventasDirectasDia(inicio: Long, fin: Long): List<VentaDirecta> = ventas.listarDelDia(inicio, fin)
    suspend fun cuentasCerradasDia(inicio: Long, fin: Long): List<CuentaConDetalles> = cuentas.cuentasCerradasDelDia(inicio, fin)
}
