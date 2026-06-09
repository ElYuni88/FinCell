package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.Cuenta
import com.tuapp.ventas.data.model.relaciones.CuentaConDetalles
import com.tuapp.ventas.data.model.relaciones.CuentaResumen
import kotlinx.coroutines.flow.Flow

@Dao
interface CuentaDao {
    @Insert suspend fun insertar(cuenta: Cuenta): Long
    @Update suspend fun actualizar(cuenta: Cuenta)
    @Query("UPDATE cuentas SET total = :total WHERE id = :cuentaId") suspend fun actualizarTotal(cuentaId: Long, total: Double)
    @Query("SELECT * FROM cuentas WHERE estado = 'ABIERTA' ORDER BY fecha_apertura DESC") fun observarAbiertas(): Flow<List<Cuenta>>
    @Query("""
        SELECT c.id AS id, c.cliente_id AS clienteId, cl.nombre AS nombreCliente,
               c.fecha_apertura AS fechaApertura, c.fecha_cierre AS fechaCierre,
               c.estado AS estado, c.total AS total,
               COALESCE(SUM(d.cantidad), 0) AS cantidadProductos
        FROM cuentas c
        INNER JOIN clientes cl ON cl.id = c.cliente_id
        LEFT JOIN detalle_cuenta d ON d.cuenta_id = c.id
        GROUP BY c.id
        ORDER BY CASE WHEN c.estado = 'ABIERTA' THEN 0 ELSE 1 END, c.fecha_apertura DESC
    """) fun observarResumenCuentas(): Flow<List<CuentaResumen>>
    @Query("SELECT * FROM cuentas WHERE id = :id") suspend fun obtener(id: Long): Cuenta?
    @Transaction @Query("SELECT * FROM cuentas WHERE id = :id") fun observarCuentaConDetalles(id: Long): Flow<CuentaConDetalles?>
    @Transaction @Query("SELECT * FROM cuentas WHERE id = :id") suspend fun obtenerCuentaConDetalles(id: Long): CuentaConDetalles?
    @Transaction @Query("SELECT * FROM cuentas WHERE estado = 'CERRADA' AND fecha_cierre BETWEEN :inicio AND :fin ORDER BY fecha_cierre ASC") suspend fun cuentasCerradasDelDia(inicio: Long, fin: Long): List<CuentaConDetalles>
    @Query("SELECT COALESCE(SUM(total), 0) FROM cuentas WHERE estado = 'CERRADA' AND fecha_cierre BETWEEN :inicio AND :fin") fun observarTotalCerradasDia(inicio: Long, fin: Long): Flow<Double>
    @Query("SELECT COUNT(*) FROM cuentas WHERE estado = 'CERRADA' AND fecha_cierre BETWEEN :inicio AND :fin") fun observarCantidadCerradasDia(inicio: Long, fin: Long): Flow<Int>
}
