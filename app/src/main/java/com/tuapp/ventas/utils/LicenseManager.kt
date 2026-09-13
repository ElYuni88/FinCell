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

    // ✅ NUEVO: Ventana de activación de 24 horas
    private const val ACTIVATION_WINDOW_MS = 24 * 60 * 60 * 1000L

    /**
     * Genera una licencia con deviceId, expiración, fecha de generación y código de transacción.
     * Formato: hash|expiracion|fechaGeneracion|transactionCode
     *
     * @param deviceId ID del dispositivo
     * @param expiracion Timestamp de expiración
     * @param transactionCode Código de transacción (o "GRATUITA" para prueba)
     * @param fechaGeneracion Timestamp de generación (por defecto: ahora)
     */
    fun generarLicencia(
        deviceId: String,
        expiracion: Long,
        transactionCode: String,
        fechaGeneracion: Long = System.currentTimeMillis()
    ): String {
        val data = "$deviceId|$expiracion|$fechaGeneracion|$transactionCode|$SECRET_KEY"
        val hash = sha256(data).take(32)
        return "$hash|$expiracion|$fechaGeneracion|$transactionCode"
    }

    /**
     * Verifica una licencia completa (al momento de activarla).
     * Formato esperado: hash|expiracion|fechaGeneracion|transactionCode
     *
     * Valida:
     * 1. Que el formato sea correcto
     * 2. Que NO haya expirado
     * 3. ✅ Que se active dentro de las primeras 24 horas desde su generación
     * 4. Que la firma (hash) sea válida
     */
    fun verifyLicense(context: Context, codigo: String): VerificationResult {
        val parts = codigo.split("|")
        android.util.Log.d("LicenseDebug", "=== VERIFICANDO LICENCIA ===")
        android.util.Log.d("LicenseDebug", "Código completo: '$codigo'")
        android.util.Log.d("LicenseDebug", "Longitud: ${codigo.length}")
        android.util.Log.d("LicenseDebug", "Partes: ${parts.size}")
        parts.forEachIndexed { i, s ->
            android.util.Log.d("LicenseDebug", "  Parte $i: '$s' (len=${s.length})")
        }

        if (parts.size != 4) {
            android.util.Log.e("LicenseDebug", "❌ Formato inválido. Se esperaban 4 partes, hay ${parts.size}")
            return VerificationResult.Invalid("Formato de licencia inválido")
        }
        val hashIngresado = parts[0]
        val expiracion = parts[1].toLongOrNull()
            ?: return VerificationResult.Invalid("Fecha de expiración inválida")
        val fechaGeneracion = parts[2].toLongOrNull()
            ?: return VerificationResult.Invalid("Fecha de generación inválida")
        val transactionCode = parts[3]

        val ahora = System.currentTimeMillis()

        // ✅ 1. Verificar expiración
        if (ahora > expiracion) {
            return VerificationResult.Invalid("La licencia ha expirado")
        }

        // ✅ 2. Verificar ventana de activación (24 horas)
        val tiempoDesdeGeneracion = ahora - fechaGeneracion
        if (tiempoDesdeGeneracion > ACTIVATION_WINDOW_MS) {
            val horasTranscurridas = tiempoDesdeGeneracion / (60 * 60 * 1000)
            return VerificationResult.Invalid(
                "La licencia debe activarse dentro de las primeras 24 horas.\n\n" +
                        "Han pasado $horasTranscurridas horas desde su generación.\n\n" +
                        "Solicita una nueva licencia al administrador."
            )
        }

        // ✅ 3. Verificar firma (hash)
        val deviceId = getDeviceId(context)
        val expectedHash = generarLicencia(deviceId, expiracion, transactionCode, fechaGeneracion)
            .split("|")[0]

        if (hashIngresado != expectedHash) {
            return VerificationResult.Invalid("Firma de licencia inválida")
        }

        return VerificationResult.Valid
    }

    /**
     * Guarda la licencia en SharedPreferences.
     */
    fun saveLicense(context: Context, codigo: String, esGratuita: Boolean = false) {
        val parts = codigo.split("|")
        if (parts.size == 4) {
            val expiracion = parts[1].toLongOrNull() ?: return
            val transactionCode = parts[3]

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
     * ⚠️ Para licencias YA GUARDADAS, NO se verifica la ventana de activación.
     * Solo verifica que no haya expirado y que la firma sea correcta.
     */
    fun hasValidLicense(context: Context): Boolean {
        val license = loadLicense(context) ?: return false
        return verifyLicenseForSaved(context, license)
    }

    /**
     * Verifica una licencia YA GUARDADA (sin ventana de activación).
     * Solo verifica expiración y firma.
     */
    private fun verifyLicenseForSaved(context: Context, codigo: String): Boolean {
        val parts = codigo.split("|")
        if (parts.size != 4) return false

        val hashIngresado = parts[0]
        val expiracion = parts[1].toLongOrNull() ?: return false
        val fechaGeneracion = parts[2].toLongOrNull() ?: return false
        val transactionCode = parts[3]

        // Solo verificar expiración
        if (System.currentTimeMillis() > expiracion) return false

        // Verificar firma
        val deviceId = getDeviceId(context)
        val expectedHash = generarLicencia(deviceId, expiracion, transactionCode, fechaGeneracion)
            .split("|")[0]

        return hashIngresado == expectedHash
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

    /**
     * Resultado de verificación de licencia.
     */
    sealed class VerificationResult {
        object Valid : VerificationResult()
        data class Invalid(val mensaje: String) : VerificationResult()
    }
}