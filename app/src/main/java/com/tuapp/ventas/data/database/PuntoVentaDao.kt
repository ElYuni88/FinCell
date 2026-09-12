package com.tuapp.ventas.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuapp.ventas.data.model.PuntoVenta
import kotlinx.coroutines.flow.Flow

@Dao
interface PuntoVentaDao {
    @Query("SELECT * FROM puntos_venta WHERE codigo = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): PuntoVenta?

    @Query("SELECT * FROM puntos_venta WHERE activo = 1 LIMIT 1")
    suspend fun obtenerActivo(): PuntoVenta?

    @Query("SELECT * FROM puntos_venta WHERE activo = 1")
    fun observarActivo(): Flow<PuntoVenta?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(pv: PuntoVenta): Long

    @Update
    suspend fun actualizar(pv: PuntoVenta)

    @Query("UPDATE puntos_venta SET activo = 0 WHERE id != :id")
    suspend fun desactivarOtros(id: Long)
}