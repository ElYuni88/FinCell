package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ventas_finales",
    foreignKeys = [ForeignKey(entity = Cuenta::class, parentColumns = ["id"], childColumns = ["cuenta_id"])],
    indices = [Index(value = ["cuenta_id"], unique = true), Index("fecha_cierre")]
)
data class VentaFinal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cuenta_id") val cuentaId: Long,
    @ColumnInfo(name = "total_venta") val totalVenta: Double,
    @ColumnInfo(name = "metodo_pago") val metodoPago: String = "EFECTIVO",
    @ColumnInfo(name = "fecha_cierre") val fechaCierre: Long = System.currentTimeMillis(),
    val observaciones: String? = null
)
