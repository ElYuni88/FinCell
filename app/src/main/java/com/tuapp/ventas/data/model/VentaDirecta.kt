package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ventas_directas",
    foreignKeys = [ForeignKey(entity = Producto::class, parentColumns = ["id"], childColumns = ["producto_id"])],
    indices = [Index("fecha_venta"), Index("producto_id")]
)
data class VentaDirecta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "producto_id") val productoId: Long,
    @ColumnInfo(name = "codigo_barras") val codigoBarras: String,
    @ColumnInfo(name = "nombre_producto") val nombreProducto: String,
    val precio: Double,
    @ColumnInfo(name = "fecha_venta") val fechaVenta: Long = System.currentTimeMillis()
)
