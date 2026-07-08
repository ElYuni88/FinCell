package com.tuapp.ventas.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuapp.ventas.data.model.Notificacion
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {
    @Query("SELECT * FROM notificaciones WHERE eliminada = 0 ORDER BY fecha_generacion DESC")
    fun observarNoEliminadas(): Flow<List<Notificacion>>

    @Query("SELECT COUNT(*) FROM notificaciones WHERE eliminada = 0 AND leida = 0")
    fun observarCantidadNoLeidas(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notificaciones WHERE eliminada = 0")
    fun observarCantidadNoEliminadas(): Flow<Int>

    @Query("SELECT * FROM notificaciones WHERE tipo = :tipo AND eliminada = 0 LIMIT 1")
    suspend fun buscarActivaPorTipo(tipo: String): Notificacion?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(notificacion: Notificacion): Long

    @Query("UPDATE notificaciones SET leida = 1 WHERE eliminada = 0")
    suspend fun marcarTodasLeidas()

    @Query("UPDATE notificaciones SET leida = 1 WHERE id IN (:ids)")
    suspend fun marcarLeidas(ids: List<Long>)

    @Query("UPDATE notificaciones SET eliminada = 1 WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("UPDATE notificaciones SET eliminada = 1 WHERE id IN (:ids)")
    suspend fun eliminar(ids: List<Long>)
}
