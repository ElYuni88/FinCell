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
    entities = [Producto::class, VentaDirecta::class, Cliente::class, Cuenta::class, DetalleCuenta::class, VentaFinal::class, Notificacion::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun ventaDirectaDao(): VentaDirectaDao
    abstract fun clienteDao(): ClienteDao
    abstract fun cuentaDao(): CuentaDao
    abstract fun detalleCuentaDao(): DetalleCuentaDao
    abstract fun ventaFinalDao(): VentaFinalDao
    abstract fun notificacionDao(): NotificacionDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys=OFF")
                database.execSQL("""
                    CREATE TABLE productos_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        codigo_barras TEXT NOT NULL,
                        nombre TEXT NOT NULL,
                        precio REAL NOT NULL,
                        tipo_producto TEXT NOT NULL DEFAULT 'CODIGO_BARRAS',
                        inventario INTEGER NOT NULL DEFAULT 0,
                        vendidos INTEGER NOT NULL DEFAULT 0,
                        esManual INTEGER NOT NULL DEFAULT 0,
                        fecha_creacion INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO productos_new (id, codigo_barras, nombre, precio, tipo_producto, inventario, vendidos, esManual, fecha_creacion)
                    SELECT id, COALESCE(NULLIF(codigo_barras, ''), 'LEGACY_' || id), nombre, precio, 'CODIGO_BARRAS', 0, 0, 0, fecha_creacion FROM productos
                """.trimIndent())
                database.execSQL("DROP TABLE productos")
                database.execSQL("ALTER TABLE productos_new RENAME TO productos")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_productos_codigo_barras ON productos(codigo_barras)")

                database.execSQL("""
                    CREATE TABLE ventas_directas_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        producto_id INTEGER NOT NULL,
                        codigo_barras TEXT NOT NULL,
                        nombre_producto TEXT NOT NULL,
                        precio REAL NOT NULL,
                        fecha_venta INTEGER NOT NULL,
                        FOREIGN KEY(producto_id) REFERENCES productos(id) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO ventas_directas_new (id, producto_id, codigo_barras, nombre_producto, precio, fecha_venta)
                    SELECT id, producto_id, COALESCE(NULLIF(codigo_barras, ''), 'LEGACY_' || producto_id), nombre_producto, precio, fecha_venta FROM ventas_directas
                """.trimIndent())
                database.execSQL("DROP TABLE ventas_directas")
                database.execSQL("ALTER TABLE ventas_directas_new RENAME TO ventas_directas")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_directas_fecha_venta ON ventas_directas(fecha_venta)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_directas_producto_id ON ventas_directas(producto_id)")
                database.execSQL("PRAGMA foreign_keys=ON")
            }
        }


        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE productos ADD COLUMN esManual INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE productos SET esManual = 1 WHERE tipo_producto = 'MANUAL' OR codigo_barras LIKE 'MANUAL_%'")
            }
        }


        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS notificaciones (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tipo TEXT NOT NULL,
                        mensaje TEXT NOT NULL,
                        fecha_generacion INTEGER NOT NULL,
                        leida INTEGER NOT NULL DEFAULT 0,
                        eliminada INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ventas_seguras.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
            Producto(codigoBarras = "750100000001", nombre = "Café Americano", precio = 35.0, inventario = 100, fechaCreacion = ahora),
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
