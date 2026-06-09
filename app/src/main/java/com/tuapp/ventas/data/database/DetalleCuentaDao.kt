package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.DetalleCuenta

@Dao
interface DetalleCuentaDao {
    @Insert suspend fun insertar(detalle: DetalleCuenta): Long
    @Query("UPDATE detalle_cuenta SET cantidad = :cantidad, subtotal = precio_unitario * :cantidad WHERE id = :detalleId") suspend fun actualizarCantidad(detalleId: Long, cantidad: Int)
    @Query("DELETE FROM detalle_cuenta WHERE id = :detalleId") suspend fun eliminar(detalleId: Long)
    @Query("SELECT * FROM detalle_cuenta WHERE id = :detalleId") suspend fun obtener(detalleId: Long): DetalleCuenta?
    @Query("SELECT COALESCE(SUM(subtotal), 0) FROM detalle_cuenta WHERE cuenta_id = :cuentaId") suspend fun totalCuenta(cuentaId: Long): Double
}
