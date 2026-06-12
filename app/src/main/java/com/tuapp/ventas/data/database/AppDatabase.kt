package com.tuapp.ventas.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuapp.ventas.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Producto::class, VentaDirecta::class, Cliente::class, Cuenta::class, DetalleCuenta::class, VentaFinal::class],
    version = 2,
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
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE clientes ADD COLUMN mesa TEXT")
                database.execSQL("ALTER TABLE clientes ADD COLUMN recordarCuenta INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE cuentas ADD COLUMN esClienteTemporal INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE cuentas ADD COLUMN nombre_cliente_temporal TEXT")
                database.execSQL("ALTER TABLE cuentas ADD COLUMN mesa_temporal TEXT")

                // Se reconstruye la tabla para permitir cuentas temporales sin cliente_id persistido.
                database.execSQL("""
                    CREATE TABLE cuentas_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cliente_id INTEGER,
                        fecha_apertura INTEGER NOT NULL,
                        fecha_cierre INTEGER,
                        estado TEXT NOT NULL,
                        total REAL NOT NULL,
                        esClienteTemporal INTEGER NOT NULL DEFAULT 0,
                        nombre_cliente_temporal TEXT,
                        mesa_temporal TEXT,
                        FOREIGN KEY(cliente_id) REFERENCES clientes(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO cuentas_new (id, cliente_id, fecha_apertura, fecha_cierre, estado, total, esClienteTemporal, nombre_cliente_temporal, mesa_temporal)
                    SELECT id, cliente_id, fecha_apertura, fecha_cierre, estado, total, esClienteTemporal, nombre_cliente_temporal, mesa_temporal
                    FROM cuentas
                """.trimIndent())
                database.execSQL("DROP TABLE cuentas")
                database.execSQL("ALTER TABLE cuentas_new RENAME TO cuentas")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cuentas_cliente_id ON cuentas(cliente_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cuentas_estado ON cuentas(estado)")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ventas_seguras.db")
                .addMigrations(MIGRATION_1_2)
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
