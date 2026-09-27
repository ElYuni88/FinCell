package com.tuapp.ventas.data.model

import com.google.gson.annotations.SerializedName

/**
 * Archivo IPV: sincronización Admin → Cliente.
 *
 * Dos usos:
 * 1. Sincronización inicial: el Admin asigna el PV al cliente (productos vacíos o catálogo completo).
 * 2. Exportación tras auditoría: el Admin envía productos aprobados con precios e inventarios finales.
 *
 * El campo `firma` contiene el SHA-256 generado con SECRET_IPV para verificar autenticidad.
 */
data class ArchivoIPV(
    @SerializedName("version") val version: String = "1.0",
    @SerializedName("fecha_exportacion") val fechaExportacion: Long = System.currentTimeMillis(),
    @SerializedName("punto_venta") val puntoVenta: PuntoVentaExport,
    @SerializedName("productos") val productos: List<ProductoIPV> = emptyList(),
    @SerializedName("es_sincronizacion_inicial") val esSincronizacionInicial: Boolean = false,
    @SerializedName("firma") val firma: String = ""
)

/**
 * Punto de Venta en formato de exportación (JSON).
 * NO es una entidad Room. La entidad es `PuntoVenta`.
 */
data class PuntoVentaExport(
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: Long = System.currentTimeMillis()
)

/**
 * Producto para importar/exportar en IPV.
 * NO es una entidad Room. La entidad es `Producto`.
 *
 * ⚠️ Los nombres de los campos deben coincidir EXACTAMENTE con los del Admin,
 *    porque la firma SHA-256 se calcula sobre el JSON serializado.
 */
data class ProductoIPV(
    @SerializedName("codigo_barras") val codigoBarras: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("precio") val precio: Double,
    @SerializedName("inventario") val inventario: Int
)

/**
 * Convierte un ProductoIPV (transportado en JSON) a la entidad Room `Producto`.
 * Como el Admin no maneja `esManual` ni `tipoProducto`, se asume producto con código de barras.
 */
fun ProductoIPV.toProducto(): Producto {
    return Producto(
        codigoBarras = this.codigoBarras,
        nombre = this.nombre,
        precio = this.precio,
        inventario = this.inventario,
        esManual = false,
        tipoProducto = Producto.TIPO_CODIGO_BARRAS
    )
}

/**
 * Convierte una entidad `PuntoVentaExport` (JSON) a la entidad Room `PuntoVenta`.
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
 * Convierte una entidad `PuntoVenta` (Room) a `PuntoVentaExport` (JSON).
 * Usado por ExportarIPBActivity para armar el IPB.
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