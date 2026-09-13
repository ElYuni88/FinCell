package com.tuapp.ventas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Notificación local generada por reglas de negocio de CafePos. */
@Entity(tableName = "notificaciones")
data class Notificacion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val mensaje: String,
    @ColumnInfo(name = "fecha_generacion") val fechaGeneracion: Long = System.currentTimeMillis(),
    val leida: Boolean = false,
    val eliminada: Boolean = false
) {
    companion object {
        const val TIPO_BAJO_INVENTARIO = "BAJO_INVENTARIO"
        const val TIPO_CUENTAS_ABIERTAS = "CUENTAS_ABIERTAS"
        const val TIPO_TRANSFERENCIAS_DIA = "TRANSFERENCIAS_DIA"
        const val TIPO_LICENCIA_EXPIRACION = "LICENCIA_EXPIRACION"

        const val TIPO_SIN_PUNTO_VENTA = "SIN_PUNTO_VENTA"

        const val TIPO_LICENCIA_DIARIA = "LICENCIA_DIARIA"

        const val TIPO_IPB_NO_EXPORTADO = "IPB_NO_EXPORTADO"
    }
}