package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.SnapshotInventario

@Dao
interface SnapshotInventarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(items: List<SnapshotInventario>)

    @Query("SELECT * FROM snapshots_inventario WHERE fecha = :fecha ORDER BY nombre ASC")
    suspend fun obtenerPorFecha(fecha: String): List<SnapshotInventario>

    @Query("SELECT COUNT(*) FROM snapshots_inventario WHERE fecha = :fecha")
    suspend fun contarPorFecha(fecha: String): Int

    @Query("SELECT MAX(fecha) FROM snapshots_inventario")
    suspend fun obtenerFechaMasReciente(): String?

    @Query("DELETE FROM snapshots_inventario WHERE fecha = :fecha")
    suspend fun eliminarPorFecha(fecha: String)

    @Query("DELETE FROM snapshots_inventario")
    suspend fun eliminarTodos()
}