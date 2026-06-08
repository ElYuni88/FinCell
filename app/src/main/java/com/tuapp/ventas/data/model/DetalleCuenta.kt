package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detalle_cuenta",
    foreignKeys = [
        ForeignKey(entity = Cuenta::class, parentColumns = ["id"], childColumns = ["cuenta_id"]),
        ForeignKey(entity = Producto::class, parentColumns = ["id"], childColumns = ["producto_id"])
    ],
    indices = [Index("cuenta_id"), Index("producto_id")]
)
data class DetalleCuenta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cuenta_id") val cuentaId: Long,
    @ColumnInfo(name = "producto_id") val productoId: Long,
    val cantidad: Int = 1,
    @ColumnInfo(name = "precio_unitario") val precioUnitario: Double,
    val subtotal: Double,
    @ColumnInfo(name = "fecha_agregado") val fechaAgregado: Long = System.currentTimeMillis()
)
