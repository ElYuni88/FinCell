package com.tuapp.ventas

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tuapp.ventas.data.database.AppDatabase
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.utils.LicenseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VentasApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: VentasRepository by lazy { VentasRepository(database, this) } // ✅ Pasar context

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificaciones()
        verificarEstadoLicencia()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "license_channel",
                "Notificaciones de licencia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones sobre expiración de licencia"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun verificarEstadoLicencia() {
        CoroutineScope(Dispatchers.IO).launch {
            // ✅ Generar notificaciones de licencia en el repositorio
            repository.generarNotificacionesLicencia()

            // ✅ 2. Notificación diaria de licencia
            repository.generarNotificacionLicenciaDiaria()

            // ✅ 3. Notificación si no hay Punto de Venta
            repository.generarNotificacionSinPuntoVenta()

            // ✅ 4. Notificación si no se exportó el IPB
            repository.generarNotificacionIPBNoExportado()

            // ✅ También verificar para notificación push (sistema)
            val daysRemaining = LicenseManager.getDaysRemaining(this@VentasApplication)
            val diaNotificacion = LicenseManager.checkExpirationWarning(this@VentasApplication)

            if (diaNotificacion != null && daysRemaining > 0) {
                mostrarNotificacionSistema(diaNotificacion, daysRemaining.toInt())
            }
        }
    }

    private fun mostrarNotificacionSistema(dia: Int, diasRestantes: Int) {
        // ✅ Verificar permiso para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // No tenemos permiso, no mostramos notificación
                return
            }
        }

        val titulo = when (dia) {
            5 -> "⏰ Tu licencia expira en 5 días"
            3 -> "⚠️ Tu licencia expira en 3 días"
            1 -> "🚨 ¡Tu licencia expira MAÑANA!"
            else -> "Tu licencia está por expirar"
        }

        val mensaje = when (dia) {
            5 -> "Renueva tu licencia para continuar usando la app sin interrupciones."
            3 -> "¡Solo quedan 3 días! Renueva ahora para evitar la suspensión del servicio."
            1 -> "¡ÚLTIMO DÍA! Renueva tu licencia hoy para no perder acceso."
            else -> "Quedan $diasRestantes días. Renueva tu licencia."
        }

        val notification = NotificationCompat.Builder(this, "license_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1000 + dia, notification)
    }
}