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
    val inactivo: Boolean = false,
    /**
     * ✅ NUEVO (v12): inventario que tenía el producto al inicio del periodo.
     * No cambia por ventas ni por reabastecimiento.
     * Se usa para calcular el "Stock Inicial" en Existencias del día.
     */
    @ColumnInfo(name = "inventario_al_inicio_periodo")
    val inventarioAlInicioPeriodo: Int = 0
) : Serializable {
    companion object {
        const val TIPO_CODIGO_BARRAS = "CODIGO_BARRAS"
        const val TIPO_MANUAL = "MANUAL"
    }
}