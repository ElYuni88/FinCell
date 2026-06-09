package com.tuapp.ventas

import android.app.Application
import com.tuapp.ventas.data.database.AppDatabase
import com.tuapp.ventas.data.repository.VentasRepository

class VentasApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: VentasRepository by lazy { VentasRepository(database) }
}
