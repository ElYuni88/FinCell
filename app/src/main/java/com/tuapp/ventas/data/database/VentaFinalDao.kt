package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.VentaFinal
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaFinalDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(venta: VentaFinal): Long
    @Query("SELECT * FROM ventas_finales WHERE fecha_cierre BETWEEN :inicio AND :fin") suspend fun listarDelDia(inicio: Long, fin: Long): List<VentaFinal>
    @Query("SELECT * FROM ventas_finales WHERE cuenta_id = :cuentaId LIMIT 1") suspend fun obtenerPorCuenta(cuentaId: Long): VentaFinal?
    @Query("SELECT COUNT(*) FROM ventas_finales WHERE UPPER(metodo_pago) = UPPER(:metodoPago) AND fecha_cierre BETWEEN :inicio AND :fin") fun observarCantidadPorMetodoDia(metodoPago: String, inicio: Long, fin: Long): Flow<Int>
}
