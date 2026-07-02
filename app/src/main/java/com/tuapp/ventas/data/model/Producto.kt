package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "productos", indices = [Index(value = ["codigo_barras"], unique = true)])
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "codigo_barras") val codigoBarras: String = "",
    val nombre: String,
    val precio: Double,
    @ColumnInfo(name = "tipo_producto") val tipoProducto: String = TIPO_CODIGO_BARRAS,
    val inventario: Int = 0,
    val vendidos: Int = 0,
    @ColumnInfo(name = "esManual") val esManual: Boolean = false,
    @ColumnInfo(name = "fecha_creacion") val fechaCreacion: Long = System.currentTimeMillis()
) {
    companion object {
        const val TIPO_CODIGO_BARRAS = "CODIGO_BARRAS"
        const val TIPO_MANUAL = "MANUAL"
    }
}
