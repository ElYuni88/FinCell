package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "puntos_venta",
    indices = [Index(value = ["codigo"], unique = true)]
)
data class PuntoVenta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String,
    val nombre: String,
    val direccion: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "activo")
    val activo: Boolean = true
)