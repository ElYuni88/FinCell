package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cuentas",
    foreignKeys = [ForeignKey(entity = Cliente::class, parentColumns = ["id"], childColumns = ["cliente_id"])],
    indices = [Index("cliente_id"), Index("estado")]
)
data class Cuenta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cliente_id") val clienteId: Long,
    @ColumnInfo(name = "fecha_apertura") val fechaApertura: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "fecha_cierre") val fechaCierre: Long? = null,
    val estado: String = ESTADO_ABIERTA,
    val total: Double = 0.0
) {
    companion object { const val ESTADO_ABIERTA = "ABIERTA"; const val ESTADO_CERRADA = "CERRADA" }
}
