package com.tuapp.ventas.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.tuapp.ventas.data.database.AppDatabase
import com.tuapp.ventas.data.model.*
import com.tuapp.ventas.data.model.relaciones.CuentaConDetalles
import com.tuapp.ventas.data.model.relaciones.CuentaResumen
import com.tuapp.ventas.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.Normalizer
import java.util.Calendar

class VentasRepository(private val db: AppDatabase) {
    private val productos = db.productoDao()
    private val ventas = db.ventaDirectaDao()
    private val clientes = db.clienteDao()
    private val cuentas = db.cuentaDao()
    private val detalles = db.detalleCuentaDao()
    private val finales = db.ventaFinalDao()
    private val notificaciones = db.notificacionDao()

    fun ventasDirectasHoy(): Flow<List<VentaDirecta>> = ventasDirectasPorFecha(System.currentTimeMillis())
    fun ventasDirectasPorFecha(fecha: Long): Flow<List<VentaDirecta>> = ventas.observarDelDia(inicioDia(fecha), finDia(fecha))
    fun totalVentasDirectasHoy(): Flow<Double> = totalVentasDirectasPorFecha(System.currentTimeMillis())
    fun totalVentasDirectasPorFecha(fecha: Long): Flow<Double> = ventas.observarTotalDia(inicioDia(fecha), finDia(fecha))
    fun cantidadVentasDirectasHoy(): Flow<Int> = cantidadVentasDirectasPorFecha(System.currentTimeMillis())
    fun cantidadVentasDirectasPorFecha(fecha: Long): Flow<Int> = ventas.observarCantidadDia(inicioDia(fecha), finDia(fecha))
    fun cuentasActivas(): Flow<List<Cuenta>> = cuentas.observarAbiertas()
    fun cantidadCuentasAbiertas(): Flow<Int> = cuentas.observarCantidadAbiertas()
    fun resumenCuentas(): Flow<List<CuentaResumen>> = cuentas.observarResumenCuentas()
    fun resumenCuentasPorFecha(fecha: Long): Flow<List<CuentaResumen>> = cuentas.observarResumenCuentasPorDia(inicioDia(fecha), finDia(fecha))
    fun totalCuentasCerradasHoy(): Flow<Double> = totalCuentasCerradasPorFecha(System.currentTimeMillis())
    fun totalCuentasCerradasPorFecha(fecha: Long): Flow<Double> = cuentas.observarTotalCerradasDia(inicioDia(fecha), finDia(fecha))
    fun cantidadCuentasCerradasHoy(): Flow<Int> = cantidadCuentasCerradasPorFecha(System.currentTimeMillis())
    fun cantidadCuentasCerradasPorFecha(fecha: Long): Flow<Int> = cuentas.observarCantidadCerradasDia(inicioDia(fecha), finDia(fecha))
    fun observarCuenta(cuentaId: Long): Flow<CuentaConDetalles?> = cuentas.observarCuentaConDetalles(cuentaId)

    suspend fun buscarProducto(codigo: String): Producto? = productos.buscarPorCodigo(codigo)
    suspend fun buscarProductoPorId(id: Long): Producto? = productos.buscarPorId(id)
    suspend fun buscarProductosManualesPorNombre(query: String): List<Producto> {
        val normalizada = normalizar(query)
        if (normalizada.length < 2) return emptyList()
        return productos.listarManuales().filter { normalizar(it.nombre).contains(normalizada) }.sortedBy { it.nombre }.take(10)
    }
    suspend fun buscarClientesPorNombre(query: String): List<Cliente> = clientes.buscarPorNombre(query)
    suspend fun crearProducto(codigo: String, nombre: String, precio: Double, inventario: Int = 0): Producto {
        val codigoNormalizado = codigo.ifBlank { generarCodigoManual() }
        val id = productos.insertar(Producto(codigoBarras = codigoNormalizado, nombre = nombre, precio = precio, inventario = inventario.coerceAtLeast(0), tipoProducto = Producto.TIPO_CODIGO_BARRAS, esManual = false))
        return productos.buscarPorCodigo(codigoNormalizado)!!.copy(id = id)
    }

    suspend fun crearProductoManual(codigoManual: String?, nombre: String, precio: Double): Producto {
        val codigoNormalizado = codigoManual?.trim()?.ifBlank { null } ?: generarCodigoManual()
        productos.buscarPorCodigo(codigoNormalizado)?.let { return it }
        val producto = Producto(codigoBarras = codigoNormalizado, nombre = nombre, precio = precio, tipoProducto = Producto.TIPO_MANUAL, esManual = true)
        val id = productos.insertar(producto)
        return producto.copy(id = id)
    }

    suspend fun guardarProducto(producto: Producto): Producto {
        Log.d(TAG, "guardarProducto: esManual recibido=${producto.esManual}, tipo=${producto.tipoProducto}, codigo=${producto.codigoBarras}")
        val codigoOriginal = producto.codigoBarras.trim()
        val codigoFinal = codigoOriginal.ifBlank { generarCodigoManual() }
        val esManualFinal = producto.esManual ||
            producto.tipoProducto == Producto.TIPO_MANUAL ||
            codigoFinal.startsWith("MANUAL_", ignoreCase = true)
        val normalizado = producto.copy(
            codigoBarras = codigoFinal,
            tipoProducto = if (esManualFinal) Producto.TIPO_MANUAL else producto.tipoProducto,
            esManual = esManualFinal
        )
        Log.d(TAG, "guardarProducto: esManual final=${normalizado.esManual}, tipo=${normalizado.tipoProducto}, codigo=${normalizado.codigoBarras}")
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
    suspend fun insertarProducto(producto: Producto): Long = guardarProducto(producto).id
    suspend fun actualizarProducto(producto: Producto) {
        guardarProducto(producto)
    }
    suspend fun buscarProductoPorCodigo(codigo: String): Producto? = productos.buscarPorCodigo(codigo)
    suspend fun existeCodigoDuplicado(codigo: String, id: Long): Boolean = codigo.isNotBlank() && productos.existeCodigoDuplicado(codigo, id) > 0
    suspend fun verificarCodigoDuplicado(codigo: String): Boolean = codigo.isNotBlank() && productos.existeCodigo(codigo) > 0
    fun obtenerProductosAgotados(): Flow<List<Producto>> = productos.obtenerAgotados()
    fun productosSinCodigo(): Flow<List<Producto>> = productos.obtenerProductosSinCodigo()
    fun productosBajoInventario(limite: Int = 5): Flow<List<Producto>> = productos.observarProductosBajoInventario(limite)
    fun cantidadTransferenciasHoy(): Flow<Int> = finales.observarCantidadPorMetodoDia("TRANSFERENCIA", DateUtils.inicioDia(), DateUtils.finDia())
    fun hayNotificaciones(): Flow<Boolean> = combine(notificaciones.observarCantidadNoLeidas(), notificaciones.observarCantidadNoEliminadas()) { noLeidas, noEliminadas -> noLeidas > 0 || noEliminadas > 0 }
    fun observarNotificaciones(): Flow<List<Notificacion>> = notificaciones.observarNoEliminadas()
    suspend fun marcarNotificacionesLeidas(ids: List<Long>) = notificaciones.marcarLeidas(ids)
    suspend fun eliminarNotificacion(id: Long) = notificaciones.eliminar(id)
    suspend fun eliminarNotificaciones(ids: List<Long>) = notificaciones.eliminar(ids)
    suspend fun generarNotificacionesSistema() {
        val bajoStock = productos.listarTodos().filter { it.inventario < 5 }
        if (bajoStock.isNotEmpty()) crearNotificacionSiNoExiste(Notificacion.TIPO_BAJO_INVENTARIO, "Productos con inventario menor a 5 unidades: ${bajoStock.joinToString { it.nombre }}")
        val abiertas = cuentas.contarAbiertas()
        if (abiertas > 6) crearNotificacionSiNoExiste(Notificacion.TIPO_CUENTAS_ABIERTAS, "Hay $abiertas cuentas abiertas simultáneamente")
        val transferencias = finales.contarPorMetodoDia("TRANSFERENCIA", DateUtils.inicioDia(), DateUtils.finDia())
        if (transferencias > 6) crearNotificacionSiNoExiste(Notificacion.TIPO_TRANSFERENCIAS_DIA, "Hay $transferencias cuentas pagadas por transferencia hoy")
    }
    fun productosAgotados(): Flow<Int> = productos.observarProductosAgotados()
    suspend fun listarProductos(): List<Producto> = productos.listarTodos()

    private suspend fun crearNotificacionSiNoExiste(tipo: String, mensaje: String) {
        if (notificaciones.buscarActivaPorTipo(tipo) == null) notificaciones.insertar(Notificacion(tipo = tipo, mensaje = mensaje))
    }

    private fun normalizar(texto: String): String = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").lowercase()

    private fun inicioDia(fecha: Long): Long = Calendar.getInstance().apply { timeInMillis = fecha; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    private fun finDia(fecha: Long): Long = Calendar.getInstance().apply { timeInMillis = fecha; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis

    companion object {
        private const val TAG = "VentasRepository"
    }

    private suspend fun generarCodigoManual(): String {
        var consecutivo = productos.listarTodos().count { it.esManual || it.tipoProducto == Producto.TIPO_MANUAL } + 1
        while (true) {
            val codigo = "MANUAL_%03d".format(consecutivo)
            if (productos.buscarPorCodigo(codigo) == null) return codigo
            consecutivo++
        }
    }
    suspend fun registrarVentaDirecta(producto: Producto, cantidad: Int = 1) {
        require(cantidad in 1..99) { "La cantidad debe estar entre 1 y 99" }
        val actualizado = productos.buscarPorId(producto.id) ?: producto
        val stockDisponible = actualizado.inventario - actualizado.vendidos
        require(cantidad <= stockDisponible) { "Stock insuficiente. Disponible: $stockDisponible" }
        repeat(cantidad) {
            ventas.insertar(VentaDirecta(productoId = producto.id, codigoBarras = producto.codigoBarras, nombreProducto = producto.nombre, precio = producto.precio))
            // solo incrementamos vendidos
            productos.incrementarVendidos(producto.id, 1)
            // NO descontamos inventario
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
        val actualizado = productos.buscarPorId(producto.id) ?: producto
        val stockDisponible = actualizado.inventario - actualizado.vendidos
        require(cantidad <= stockDisponible) { "Stock insuficiente. Disponible: $stockDisponible" }
        val cuenta = cuentas.obtener(cuentaId) ?: error("Cuenta no encontrada")
        require(cuenta.estado == Cuenta.ESTADO_ABIERTA) { "No se puede modificar una cuenta cerrada" }
        val subtotal = producto.precio * cantidad
        detalles.insertar(DetalleCuenta(cuentaId = cuentaId, productoId = producto.id, cantidad = cantidad, precioUnitario = producto.precio, subtotal = subtotal))
        // solo incrementamos vendidos
        productos.incrementarVendidos(producto.id, cantidad)
        // NO descontamos inventario
        recalcularTotal(cuentaId)
    }

    suspend fun actualizarCantidadDetalle(detalleId: Long, nuevaCantidad: Int) {
        require(nuevaCantidad in 1..99) { "La cantidad debe estar entre 1 y 99" }
        val detalle = detalles.obtener(detalleId) ?: error("Producto no encontrado en la cuenta")
        val cuenta = cuentas.obtener(detalle.cuentaId) ?: error("Cuenta no encontrada")
        require(cuenta.estado == Cuenta.ESTADO_ABIERTA) { "No se puede modificar una cuenta cerrada" }
        val producto = productos.buscarPorId(detalle.productoId) ?: error("Producto no encontrado")

        if (nuevaCantidad > detalle.cantidad) {
            val incremento = nuevaCantidad - detalle.cantidad
            val stockDisponible = producto.inventario - producto.vendidos
            require(incremento <= stockDisponible) { "No hay suficiente stock. Disponible: $stockDisponible" }
        } else {
            val decremento = detalle.cantidad - nuevaCantidad
            require(decremento <= producto.vendidos) { "No se puede devolver más de lo vendido (${producto.vendidos})" }
        }

        db.withTransaction {
            val diferencia = nuevaCantidad - detalle.cantidad
            detalles.actualizarCantidad(detalleId, nuevaCantidad)
            if (diferencia != 0) {
                productos.incrementarVendidos(detalle.productoId, diferencia)
            }
            recalcularTotal(detalle.cuentaId)
        }
    }

    suspend fun eliminarDetalle(detalleId: Long) {
        val detalle = detalles.obtener(detalleId) ?: error("Producto no encontrado en la cuenta")
        val cuenta = cuentas.obtener(detalle.cuentaId) ?: error("Cuenta no encontrada")
        require(cuenta.estado == Cuenta.ESTADO_ABIERTA) { "No se puede modificar una cuenta cerrada" }

        db.withTransaction {
            detalles.eliminar(detalleId)
            productos.incrementarVendidos(detalle.productoId, -detalle.cantidad)
            recalcularTotal(detalle.cuentaId)
        }
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
