package com.tuapp.ventas.data.repository

import com.tuapp.ventas.data.database.AppDatabase
import com.tuapp.ventas.data.model.*
import com.tuapp.ventas.data.model.relaciones.CuentaConDetalles
import com.tuapp.ventas.data.model.relaciones.CuentaResumen
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
    fun cantidadCuentasAbiertas(): Flow<Int> = cuentas.observarCantidadAbiertas()
    fun resumenCuentas(): Flow<List<CuentaResumen>> = cuentas.observarResumenCuentas()
    fun totalCuentasCerradasHoy(): Flow<Double> = cuentas.observarTotalCerradasDia(DateUtils.inicioDia(), DateUtils.finDia())
    fun cantidadCuentasCerradasHoy(): Flow<Int> = cuentas.observarCantidadCerradasDia(DateUtils.inicioDia(), DateUtils.finDia())
    fun observarCuenta(cuentaId: Long): Flow<CuentaConDetalles?> = cuentas.observarCuentaConDetalles(cuentaId)

    suspend fun buscarProducto(codigo: String): Producto? = productos.buscarPorCodigo(codigo)
    suspend fun buscarClientesPorNombre(query: String): List<Cliente> = clientes.buscarPorNombre(query)
    suspend fun crearProducto(codigo: String, nombre: String, precio: Double): Producto {
        val codigoNormalizado = codigo.ifBlank { generarCodigoManual() }
        val id = productos.insertar(Producto(codigoBarras = codigoNormalizado, nombre = nombre, precio = precio, tipoProducto = Producto.TIPO_CODIGO_BARRAS))
        return productos.buscarPorCodigo(codigoNormalizado)!!.copy(id = id)
    }

    suspend fun crearProductoManual(codigoManual: String?, nombre: String, precio: Double): Producto {
        val codigoNormalizado = codigoManual?.trim()?.ifBlank { null } ?: generarCodigoManual()
        productos.buscarPorCodigo(codigoNormalizado)?.let { return it }
        val producto = Producto(codigoBarras = codigoNormalizado, nombre = nombre, precio = precio, tipoProducto = Producto.TIPO_MANUAL)
        val id = productos.insertar(producto)
        return producto.copy(id = id)
    }

    suspend fun guardarProducto(producto: Producto): Producto {
        val codigoFinal = producto.codigoBarras.ifBlank { generarCodigoManual() }
        val normalizado = producto.copy(codigoBarras = codigoFinal, tipoProducto = if (producto.codigoBarras.isBlank()) Producto.TIPO_MANUAL else producto.tipoProducto)
        val existente = productos.buscarPorCodigo(codigoFinal)
        require(existente == null || existente.id == producto.id) { "Ya existe un producto con ese código" }
        return if (normalizado.id == 0L) {
            val id = productos.insertar(normalizado)
            normalizado.copy(id = id)
        } else {
            productos.actualizar(normalizado)
            normalizado
        }
    }

    suspend fun eliminarProducto(producto: Producto) = productos.eliminar(producto)
    fun observarProductos(): Flow<List<Producto>> = productos.observarTodos()
    fun obtenerTodosLosProductos(): Flow<List<Producto>> = productos.obtenerTodos()
    suspend fun insertarProducto(producto: Producto): Long = productos.insertar(producto)
    suspend fun actualizarProducto(producto: Producto) = productos.actualizar(producto)
    suspend fun buscarProductoPorCodigo(codigo: String): Producto? = productos.buscarPorCodigo(codigo)
    suspend fun existeCodigoDuplicado(codigo: String, id: Long): Boolean = codigo.isNotBlank() && productos.existeCodigoDuplicado(codigo, id) > 0
    fun obtenerProductosAgotados(): Flow<List<Producto>> = productos.obtenerAgotados()
    fun productosSinCodigo(): Flow<List<Producto>> = productos.obtenerProductosSinCodigo()
    fun productosAgotados(): Flow<Int> = productos.observarProductosAgotados()
    suspend fun listarProductos(): List<Producto> = productos.listarTodos()

    private suspend fun generarCodigoManual(): String {
        var consecutivo = productos.listarTodos().count { it.tipoProducto == Producto.TIPO_MANUAL } + 1
        while (true) {
            val codigo = "MANUAL_%03d".format(consecutivo)
            if (productos.buscarPorCodigo(codigo) == null) return codigo
            consecutivo++
        }
    }
    suspend fun registrarVentaDirecta(producto: Producto, cantidad: Int = 1) {
        require(cantidad in 1..99) { "La cantidad debe estar entre 1 y 99" }
        repeat(cantidad) {
            ventas.insertar(VentaDirecta(productoId = producto.id, codigoBarras = producto.codigoBarras, nombreProducto = producto.nombre, precio = producto.precio))
            productos.descontarInventario(producto.id, 1)
            productos.incrementarVendidos(producto.id, 1)
        }
    }
    suspend fun crearCuenta(nombreCliente: String, telefono: String?, mesa: String? = null, recordarCuenta: Boolean = true): Long {
        val clienteId = clientes.insertar(
            Cliente(
                nombre = nombreCliente.ifBlank { "Cliente" },
                telefono = telefono?.ifBlank { null },
                mesa = mesa?.ifBlank { null },
                recordarCuenta = recordarCuenta,
                esFrecuente = recordarCuenta
            )
        )
        return cuentas.insertar(Cuenta(clienteId = clienteId, esClienteTemporal = false))
    }

    suspend fun crearCuentaTemporal(nombreCliente: String, mesa: String?): Long {
        return cuentas.insertar(
            Cuenta(
                clienteId = null,
                esClienteTemporal = true,
                nombreClienteTemporal = nombreCliente.ifBlank { "Cliente temporal" },
                mesaTemporal = mesa?.ifBlank { null }
            )
        )
    }
    suspend fun agregarProductoACuenta(cuentaId: Long, producto: Producto, cantidad: Int) {
        require(cantidad in 1..99) { "La cantidad debe estar entre 1 y 99" }
        val cuenta = cuentas.obtener(cuentaId) ?: error("Cuenta no encontrada")
        require(cuenta.estado == Cuenta.ESTADO_ABIERTA) { "No se puede modificar una cuenta cerrada" }
        val subtotal = producto.precio * cantidad
        detalles.insertar(DetalleCuenta(cuentaId = cuentaId, productoId = producto.id, cantidad = cantidad, precioUnitario = producto.precio, subtotal = subtotal))
        productos.descontarInventario(producto.id, cantidad)
        productos.incrementarVendidos(producto.id, cantidad)
        recalcularTotal(cuentaId)
    }

    suspend fun actualizarCantidadDetalle(detalleId: Long, nuevaCantidad: Int) {
        require(nuevaCantidad in 1..99) { "La cantidad debe estar entre 1 y 99" }
        val detalle = detalles.obtener(detalleId) ?: error("Producto no encontrado en la cuenta")
        val cuenta = cuentas.obtener(detalle.cuentaId) ?: error("Cuenta no encontrada")
        require(cuenta.estado == Cuenta.ESTADO_ABIERTA) { "No se puede modificar una cuenta cerrada" }
        detalles.actualizarCantidad(detalleId, nuevaCantidad)
        recalcularTotal(detalle.cuentaId)
    }

    suspend fun eliminarDetalle(detalleId: Long) {
        val detalle = detalles.obtener(detalleId) ?: error("Producto no encontrado en la cuenta")
        val cuenta = cuentas.obtener(detalle.cuentaId) ?: error("Cuenta no encontrada")
        require(cuenta.estado == Cuenta.ESTADO_ABIERTA) { "No se puede modificar una cuenta cerrada" }
        detalles.eliminar(detalleId)
        recalcularTotal(detalle.cuentaId)
    }

    private suspend fun recalcularTotal(cuentaId: Long) {
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
    suspend fun obtenerCuenta(cuentaId: Long): Cuenta? = cuentas.obtener(cuentaId)
    suspend fun eliminarCuentaVacia(cuentaId: Long) {
        val cuenta = cuentas.obtener(cuentaId) ?: error("Cuenta no encontrada")
        require(cuenta.estado == Cuenta.ESTADO_ABIERTA) { "Solo se pueden eliminar cuentas abiertas" }
        require(cuenta.total == 0.0) { "Solo se pueden eliminar cuentas sin productos" }
        cuentas.eliminar(cuenta)
    }
    suspend fun obtenerVentaFinal(cuentaId: Long): VentaFinal? = finales.obtenerPorCuenta(cuentaId)
    suspend fun ventasDirectasDia(inicio: Long, fin: Long): List<VentaDirecta> = ventas.listarDelDia(inicio, fin)
    suspend fun cuentasCerradasDia(inicio: Long, fin: Long): List<CuentaConDetalles> = cuentas.cuentasCerradasDelDia(inicio, fin)
}
