package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.DetalleCuenta

@Dao
interface DetalleCuentaDao {
    @Insert suspend fun insertar(detalle: DetalleCuenta): Long
    @Query("SELECT COALESCE(SUM(subtotal), 0) FROM detalle_cuenta WHERE cuenta_id = :cuentaId") suspend fun totalCuenta(cuentaId: Long): Double
}
