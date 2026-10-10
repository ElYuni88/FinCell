package com.tuapp.ventas.data.model

import com.google.gson.annotations.SerializedName

/**
 * Archivo IPV: sincronización Admin → Cliente.
 *
 * ⚠️ VERSIÓN 1.1 — se añadió `inactivo` a ProductoIPV.
 *    El orden y los @SerializedName deben ser IDÉNTICOS al Admin o la firma fallará.
 */
data class ArchivoIPV(
    @SerializedName("version") val version: String = "1.1",
    @SerializedName("fecha_exportacion") val fechaExportacion: Long = System.currentTimeMillis(),
    @SerializedName("punto_venta") val puntoVenta: PuntoVentaExport,
    @SerializedName("productos") val productos: List<ProductoIPV> = emptyList(),
    @SerializedName("es_sincronizacion_inicial") val esSincronizacionInicial: Boolean = false,
    @SerializedName("firma") val firma: String = ""
)

data class PuntoVentaExport(
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: Long = System.currentTimeMillis()
)

/**
 * ⚠️ IMPORTANTE: el orden de los campos y sus @SerializedName deben coincidir
 *    EXACTAMENTE con la clase ProductoIPV del Admin. Cualquier diferencia
 *    cambia el JSON y rompe la firma SHA-256.
 */
data class ProductoIPV(
    @SerializedName("codigo_barras") val codigoBarras: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("precio") val precio: Double,
    @SerializedName("inventario") val inventario: Int,
    @SerializedName("inactivo") val inactivo: Boolean = false
)

fun ProductoIPV.toProducto(): Producto {
    return Producto(
        codigoBarras = this.codigoBarras,
        nombre = this.nombre,
        precio = this.precio,
        inventario = this.inventario,
        esManual = false,
        tipoProducto = Producto.TIPO_CODIGO_BARRAS,
        inactivo = this.inactivo
    )
}

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