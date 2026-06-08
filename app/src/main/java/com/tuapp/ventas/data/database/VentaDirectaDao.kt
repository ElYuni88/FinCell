package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.VentaDirecta
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDirectaDao {
    @Insert suspend fun insertar(venta: VentaDirecta): Long
    @Query("SELECT * FROM ventas_directas WHERE fecha_venta BETWEEN :inicio AND :fin ORDER BY fecha_venta DESC") fun observarDelDia(inicio: Long, fin: Long): Flow<List<VentaDirecta>>
    @Query("SELECT * FROM ventas_directas WHERE fecha_venta BETWEEN :inicio AND :fin ORDER BY fecha_venta ASC") suspend fun listarDelDia(inicio: Long, fin: Long): List<VentaDirecta>
    @Query("SELECT COALESCE(SUM(precio), 0) FROM ventas_directas WHERE fecha_venta BETWEEN :inicio AND :fin") fun observarTotalDia(inicio: Long, fin: Long): Flow<Double>
    @Query("SELECT COUNT(*) FROM ventas_directas WHERE fecha_venta BETWEEN :inicio AND :fin") fun observarCantidadDia(inicio: Long, fin: Long): Flow<Int>
}
