package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: Usuario): Long

    @Update
    suspend fun actualizar(usuario: Usuario)

    @Delete
    suspend fun eliminar(usuario: Usuario)

    @Query("SELECT * FROM usuarios ORDER BY es_sistema DESC, nombre ASC")
    fun observarTodos(): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios WHERE activo = 1 ORDER BY es_sistema DESC, nombre ASC")
    suspend fun listarActivos(): List<Usuario>

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): Usuario?

    @Query("SELECT * FROM usuarios WHERE telefono = :telefono LIMIT 1")
    suspend fun buscarPorTelefono(telefono: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios WHERE es_sistema = 0")
    suspend fun contarUsuariosReales(): Int

    /** Elimina solo si no es sistema. El "Sistema" nunca se puede borrar. */
    @Query("DELETE FROM usuarios WHERE id = :id AND es_sistema = 0")
    suspend fun eliminarPorId(id: Long)
}