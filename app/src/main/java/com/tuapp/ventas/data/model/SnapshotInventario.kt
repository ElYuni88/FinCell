package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "snapshots_inventario",
    primaryKeys = ["fecha", "productoId"],
    indices = [Index("fecha")]
)
data class SnapshotInventario(
    @ColumnInfo(name = "fecha") val fecha: String,
    @ColumnInfo(name = "productoId") val productoId: Long,
    val nombre: String,
    @ColumnInfo(name = "codigoBarras") val codigoBarras: String,
    val precio: Double,
    val inventario: Int,
    val vendidos: Int,
    /**
     * ✅ NUEVO (v12): inventario al inicio del periodo. Se copia del producto.
     * Es el valor que la UI muestra como "Stock Inicial".
     */
    @ColumnInfo(name = "inventario_al_inicio_periodo")
    val inventarioAlInicioPeriodo: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)