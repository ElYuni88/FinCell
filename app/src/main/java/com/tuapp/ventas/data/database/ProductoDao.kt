package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE codigo_barras = :codigo LIMIT 1") suspend fun buscarPorCodigo(codigo: String): Producto?
    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1") suspend fun buscarPorId(id: Long): Producto?
    @Query("SELECT * FROM productos ORDER BY nombre") fun observarTodos(): Flow<List<Producto>>
    @Query("SELECT * FROM productos ORDER BY nombre") suspend fun listarTodos(): List<Producto>
    @Query("SELECT * FROM productos WHERE tipo_producto = 'MANUAL' ORDER BY nombre ASC") fun obtenerProductosManuales(): Flow<List<Producto>>
    @Query("SELECT * FROM productos WHERE tipo_producto = 'MANUAL' OR codigo_barras IS NULL OR codigo_barras = '' ORDER BY nombre ASC") fun obtenerProductosSinCodigo(): Flow<List<Producto>>
    @Query("SELECT * FROM productos WHERE tipo_producto = 'MANUAL' OR codigo_barras IS NULL OR codigo_barras = '' ORDER BY nombre ASC") suspend fun listarProductosSinCodigo(): List<Producto>
    @Query("SELECT COUNT(*) FROM productos WHERE inventario = 0") fun observarProductosAgotados(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(producto: Producto): Long
    @Update suspend fun actualizar(producto: Producto)
    @Delete suspend fun eliminar(producto: Producto)
    @Query("UPDATE productos SET inventario = MAX(inventario - :cantidad, 0) WHERE id = :productoId") suspend fun descontarInventario(productoId: Long, cantidad: Int)
    @Query("UPDATE productos SET vendidos = vendidos + :cantidad WHERE id = :productoId") suspend fun incrementarVendidos(productoId: Long, cantidad: Int)
}
