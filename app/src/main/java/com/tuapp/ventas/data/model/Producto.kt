package com.tuapp.ventas.data.model

import java.io.Serializable
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
    @ColumnInfo(name = "fecha_creacion") val fechaCreacion: Long = System.currentTimeMillis(),
    /**
     * true = producto inactivo (lo inactivó el admin).
     * El cliente NO puede inactivar, solo ACTIVAR (si el producto se vuelve a usar).
     * Los productos inactivos no se pueden vender, pero si el operador escanea uno
     * y decide activarlo, se reactiva y continúa la operación.
     */
    val inactivo: Boolean = false
) : Serializable {
    companion object {
        const val TIPO_CODIGO_BARRAS = "CODIGO_BARRAS"
        const val TIPO_MANUAL = "MANUAL"
    }
}