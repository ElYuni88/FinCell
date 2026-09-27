package com.tuapp.ventas.data.database

import androidx.room.*
import com.tuapp.ventas.data.model.IpvImportado
import kotlinx.coroutines.flow.Flow

@Dao
interface IpvImportadoDao {

    @Insert
    suspend fun insertar(registro: IpvImportado): Long

    @Query("SELECT * FROM ipv_importados ORDER BY fechaImportacion DESC")
    fun observarTodos(): Flow<List<IpvImportado>>

    @Query("SELECT * FROM ipv_importados WHERE firma = :firma LIMIT 1")
    suspend fun buscarPorFirma(firma: String): IpvImportado?
}