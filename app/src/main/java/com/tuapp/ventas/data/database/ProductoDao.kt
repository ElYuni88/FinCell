package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE codigo_barras = :codigo LIMIT 1") suspend fun buscarPorCodigo(codigo: String): Producto?
    @Query("SELECT * FROM productos ORDER BY nombre") fun observarTodos(): Flow<List<Producto>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(producto: Producto): Long
    @Update suspend fun actualizar(producto: Producto)
}
