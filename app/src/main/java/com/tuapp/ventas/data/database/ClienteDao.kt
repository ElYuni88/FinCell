package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.Cliente

@Dao
interface ClienteDao {
    @Insert suspend fun insertar(cliente: Cliente): Long
    @Query("SELECT * FROM clientes WHERE id = :id") suspend fun obtener(id: Long): Cliente?
    @Query("SELECT * FROM clientes ORDER BY nombre") suspend fun listar(): List<Cliente>
}
