package com.tuapp.ventas.data.model

/**
 * Archivo JSON exportado/importado para sincronización entre Admin y Cliente.
 *
 * FLUJO:
 * 1. APK Cliente exporta IPV → lo envía al Admin
 * 2. Admin revisa, modifica y valida el IPV
 * 3. APK Cliente importa el IPV validado → actualiza su sistema
 *
 * @property version Versión del formato (ej. "1.0")
 * @property fechaExportacion Timestamp de cuando se exportó el archivo
 * @property puntoVenta Información del punto de venta (NO es entidad Room)
 * @property productos Lista de productos a importar/exportar
 * @property resumen Resumen del día (reutiliza ResumenIPB de ArchivoIPB.kt)
 * @property hashTotal Hash para verificar integridad (opcional)
 */
data class ArchivoIPV(
    val version: String = "1.0",
    val fechaExportacion: Long = System.currentTimeMillis(),
    val puntoVenta: PuntoVentaExport,
    val productos: List<ProductoIPV> = emptyList(),
    val resumen: ResumenIPB? = null,  // ← REUTILIZA ResumenIPB existente
    val hashTotal: String? = null
)

/**
 * Versión de exportación de Punto de Venta.
 *
 * ⚠️ IMPORTANTE: Esta clase NO es una entidad Room.
 * Solo se usa para serializar/deserializar JSON.
 * La entidad Room es PuntoVenta (en PuntoVenta.kt)
 *
 * @property codigo Código único del punto de venta (ej. "PV-001")
 * @property nombre Nombre del establecimiento
 * @property direccion Dirección física (opcional)
 * @property telefono Teléfono de contacto (opcional)
 * @property email Email de contacto (opcional)
 * @property fechaCreacion Timestamp de creación
 */
data class PuntoVentaExport(
    val codigo: String,
    val nombre: String,
    val direccion: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis()
)

/**
 * Producto para importar/exportar en IPV.
 *
 * ⚠️ IMPORTANTE: Esta clase NO es una entidad Room.
 * Solo se usa para serializar/deserializar JSON.
 * La entidad Room es Producto (en Producto.kt)
 *
 * @property codigoBarras Código de barras del producto
 * @property nombre Nombre del producto
 * @property precio Precio unitario
 * @property inventario Cantidad en inventario
 * @property esManual Indica si es un producto manual (sin código de barras real)
 * @property tipoProducto Tipo de producto (CODIGO_BARRAS o MANUAL)
 */
data class ProductoIPV(
    val codigoBarras: String,
    val nombre: String,
    val precio: Double,
    val inventario: Int,
    val esManual: Boolean = false,
    val tipoProducto: String = Producto.TIPO_CODIGO_BARRAS
)

/**
 * Función de extensión para convertir ProductoIPV a Producto (entidad Room)
 */
fun ProductoIPV.toProducto(): Producto {
    return Producto(
        codigoBarras = this.codigoBarras,
        nombre = this.nombre,
        precio = this.precio,
        inventario = this.inventario,
        esManual = this.esManual,
        tipoProducto = this.tipoProducto
    )
}

/**
 * Función de extensión para convertir Producto (entidad Room) a ProductoIPV
 */
fun Producto.toProductoIPV(): ProductoIPV {
    return ProductoIPV(
        codigoBarras = this.codigoBarras,
        nombre = this.nombre,
        precio = this.precio,
        inventario = this.inventario,
        esManual = this.esManual,
        tipoProducto = this.tipoProducto
    )
}

/**
 * Función de extensión para convertir PuntoVentaExport a PuntoVenta (entidad Room)
 */
fun PuntoVentaExport.toPuntoVenta(): PuntoVenta {
    return PuntoVenta(
        codigo = this.codigo,
        nombre = this.nombre,
        direccion = this.direccion,
        telefono = this.telefono,
        email = this.email,
        fechaCreacion = this.fechaCreacion
    )
}

/**
 * Función de extensión para convertir PuntoVenta (entidad Room) a PuntoVentaExport
 */
fun PuntoVenta.toPuntoVentaExport(): PuntoVentaExport {
    return PuntoVentaExport(
        codigo = this.codigo,
        nombre = this.nombre,
        direccion = this.direccion,
        telefono = this.telefono,
        email = this.email,
        fechaCreacion = this.fechaCreacion
    )
}