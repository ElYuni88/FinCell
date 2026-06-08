package com.tuapp.ventas.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuapp.ventas.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Producto::class, VentaDirecta::class, Cliente::class, Cuenta::class, DetalleCuenta::class, VentaFinal::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun ventaDirectaDao(): VentaDirectaDao
    abstract fun clienteDao(): ClienteDao
    abstract fun cuentaDao(): CuentaDao
    abstract fun detalleCuentaDao(): DetalleCuentaDao
    abstract fun ventaFinalDao(): VentaFinalDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ventas_seguras.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch { INSTANCE?.precargarProductosDemo() }
                    }
                })
                .build().also { INSTANCE = it }
        }
    }

    private suspend fun precargarProductosDemo() {
        val dao = productoDao()
        val ahora = System.currentTimeMillis()
        listOf(
            Producto(codigoBarras = "750100000001", nombre = "Café Americano", precio = 35.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000002", nombre = "Capuchino", precio = 48.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000003", nombre = "Latte", precio = 52.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000004", nombre = "Té Chai", precio = 45.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000005", nombre = "Chocolate Caliente", precio = 50.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000006", nombre = "Croissant", precio = 38.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000007", nombre = "Muffin de Blueberry", precio = 42.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000008", nombre = "Sandwich Panini", precio = 85.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000009", nombre = "Agua Mineral", precio = 25.0, fechaCreacion = ahora),
            Producto(codigoBarras = "750100000010", nombre = "Cerveza Corona", precio = 55.0, fechaCreacion = ahora)
        ).forEach { runCatching { dao.insertar(it) } }
    }
}
