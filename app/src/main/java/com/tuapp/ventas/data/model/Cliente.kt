package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val telefono: String? = null,
    val mesa: String? = null,
    val recordarCuenta: Boolean = false,
    @ColumnInfo(name = "fecha_registro") val fechaRegistro: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "es_frecuente") val esFrecuente: Boolean = false
)
