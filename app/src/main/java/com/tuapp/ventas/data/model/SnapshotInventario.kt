package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Snapshot diario del inventario.
 *
 * Se guarda el estado del stock al momento del snapshot.
 * Guardamos `inventario` y `vendidos` por separado para tener flexibilidad
 * y poder recalcular el stock disponible en cualquier momento.
 *
 * Clave primaria compuesta (fecha, productoId) → permite múltiples productos por día
 * y reemplazar (REPLACE) si se regenera el mismo día.
 */
@Entity(
    tableName = "snapshots_inventario",
    primaryKeys = ["fecha", "productoId"],
    indices = [Index("fecha")]
)
data class SnapshotInventario(
    @ColumnInfo(name = "fecha") val fecha: String,           // "2026-10-13" (formato archivo)
    @ColumnInfo(name = "productoId") val productoId: Long,
    val nombre: String,
    @ColumnInfo(name = "codigoBarras") val codigoBarras: String,
    val precio: Double,
    val inventario: Int,
    val vendidos: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)