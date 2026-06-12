package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cuentas",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("cliente_id"), Index("estado")]
)
data class Cuenta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cliente_id") val clienteId: Long? = null,
    @ColumnInfo(name = "fecha_apertura") val fechaApertura: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "fecha_cierre") val fechaCierre: Long? = null,
    val estado: String = ESTADO_ABIERTA,
    val total: Double = 0.0,
    val esClienteTemporal: Boolean = false,
    @ColumnInfo(name = "nombre_cliente_temporal") val nombreClienteTemporal: String? = null,
    @ColumnInfo(name = "mesa_temporal") val mesaTemporal: String? = null
) {
    companion object { const val ESTADO_ABIERTA = "ABIERTA"; const val ESTADO_CERRADA = "CERRADA" }
}
