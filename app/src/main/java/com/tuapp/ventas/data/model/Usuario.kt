package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["telefono"], unique = true)]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val telefono: String,
    @ColumnInfo(name = "codigo_pais") val codigoPais: String = "+53",
    val pin: String? = null,
    @ColumnInfo(name = "pin_requerido") val pinRequerido: Boolean = false,
    val activo: Boolean = true,
    /**
     * true = usuario interno "Sistema" que recibe las ventas legacy
     * (de versiones anteriores sin usuarios). No se puede eliminar.
     */
    @ColumnInfo(name = "es_sistema") val esSistema: Boolean = false,
    @ColumnInfo(name = "fecha_creacion") val fechaCreacion: Long = System.currentTimeMillis()
) {
    val nombreMostrar: String get() = if (esSistema) "Sistema" else nombre
}