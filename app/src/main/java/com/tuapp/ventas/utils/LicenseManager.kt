package com.tuapp.ventas.utils

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.security.MessageDigest

object LicenseManager {

    private const val SECRET_KEY = "MiClaveSecretaMuyLarga2026"
    private const val PREF_NAME = "license_prefs"
    private const val KEY_LICENSE = "license_code"
    private const val KEY_EXPIRATION = "license_expiration"
    private const val KEY_TRANSACTION = "license_transaction"
    private const val KEY_IS_FREE_TRIAL = "license_is_free_trial"
    private const val KEY_LAST_NOTIFICATION = "last_notification_day"

    /**
     * Genera una licencia con deviceId, expiración y código de transacción.
     * Formato: hash|expiracion|transaccion
     *
     * @param deviceId ID del dispositivo
     * @param expiracion Timestamp de expiración
     * @param transactionCode Código de transacción (o "GRATUITA" para prueba)
     */
    fun generarLicencia(deviceId: String, expiracion: Long, transactionCode: String): String {
        val data = "$deviceId|$expiracion|$transactionCode|$SECRET_KEY"
        val hash = sha256(data).take(32)
        return "$hash|$expiracion|$transactionCode"
    }

    /**
     * Verifica una licencia completa.
     * Formato esperado: hash|expiracion|transaccion
     */
    fun verifyLicense(context: Context, codigo: String): Boolean {
        val parts = codigo.split("|")
        if (parts.size != 3) return false

        val hashIngresado = parts[0]
        val expiracion = parts[1].toLongOrNull() ?: return false
        val transactionCode = parts[2]

        // Verificar expiración
        if (System.currentTimeMillis() > expiracion) return false

        val deviceId = getDeviceId(context)
        val expectedHash = generarLicencia(deviceId, expiracion, transactionCode).split("|")[0]
        return hashIngresado == expectedHash
    }

    /**
     * Guarda la licencia en SharedPreferences.
     */
    fun saveLicense(context: Context, codigo: String, esGratuita: Boolean = false) {
        val parts = codigo.split("|")
        if (parts.size == 3) {
            val expiracion = parts[1].toLongOrNull() ?: return
            val transactionCode = parts[2]

            val prefs = getPrefs(context)
            prefs.edit().apply {
                putString(KEY_LICENSE, codigo)
                putLong(KEY_EXPIRATION, expiracion)
                putString(KEY_TRANSACTION, transactionCode)
                putBoolean(KEY_IS_FREE_TRIAL, esGratuita)
                putInt(KEY_LAST_NOTIFICATION, 0)
                apply()
            }
        }
    }

    /**
     * Carga la licencia guardada.
     */
    fun loadLicense(context: Context): String? {
        return getPrefs(context).getString(KEY_LICENSE, null)
    }

    /**
     * Obtiene el código de transacción guardado.
     */
    fun getTransactionCode(context: Context): String? {
        return getPrefs(context).getString(KEY_TRANSACTION, null)
    }

    /**
     * Verifica si la licencia actual es una prueba gratuita.
     */
    fun isFreeTrial(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_FREE_TRIAL, false)
    }

    /**
     * Obtiene la fecha de expiración.
     */
    fun getExpiration(context: Context): Long {
        return getPrefs(context).getLong(KEY_EXPIRATION, 0L)
    }

    /**
     * Verifica si hay una licencia válida guardada.
     */
    fun hasValidLicense(context: Context): Boolean {
        val license = loadLicense(context) ?: return false
        return verifyLicense(context, license)
    }

    /**
     * Obtiene el ID del dispositivo actual.
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    /**
     * Calcula los días restantes de la licencia.
     */
    fun getDaysRemaining(context: Context): Long {
        val expiracion = getExpiration(context)
        if (expiracion == 0L) return 0
        val ahora = System.currentTimeMillis()
        return (expiracion - ahora) / (24 * 60 * 60 * 1000)
    }

    /**
     * Verifica si debe mostrar notificación de expiración.
     */
    fun checkExpirationWarning(context: Context): Int? {
        val daysRemaining = getDaysRemaining(context)
        if (daysRemaining <= 0) return null

        val prefs = getPrefs(context)
        val lastNotified = prefs.getInt(KEY_LAST_NOTIFICATION, 0)

        val diasNotificar = listOf(5, 3, 1)
        for (dia in diasNotificar) {
            if (daysRemaining <= dia && lastNotified < dia) {
                prefs.edit().putInt(KEY_LAST_NOTIFICATION, dia).apply()
                return dia
            }
        }
        return null
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}