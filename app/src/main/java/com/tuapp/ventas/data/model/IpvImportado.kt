package com.tuapp.ventas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro de cada IPV importado exitosamente.
 * Sirve para detectar re-importaciones del mismo archivo y para historial.
 */
@Entity(tableName = "ipv_importados")
data class IpvImportado(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombreArchivo: String,
    val firma: String,
    val codigoPv: String,
    val fechaExportacion: Long,
    val fechaImportacion: Long = System.currentTimeMillis(),
    val esSincronizacionInicial: Boolean,
    val cantidadProductos: Int,
    val creados: Int,
    val actualizados: Int,
    val errores: Int
)