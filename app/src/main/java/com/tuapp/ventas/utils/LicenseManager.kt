package com.tuapp.ventas.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object LicenseManager {

    private const val SECRET_KEY = "MiClaveSecretaMuyLarga2026"
    private const val PREF_NAME = "license_prefs"
    private const val KEY_LICENSE = "license_code"
    private const val KEY_EXPIRATION = "license_expiration"
    private const val KEY_POS_ID = "license_pos_id"

    /**
     * Genera una clave de licencia incluyendo el ID del punto de venta.
     * Formato: hash|timestamp
     */
    fun generarLicencia(deviceId: String, posId: String, expiracion: Long): String {
        val data = "$deviceId|$posId|$expiracion|$SECRET_KEY"
        val hash = sha256(data).take(32)
        return "$hash|$expiracion"
    }

    /**
     * Verifica una licencia completa (hash|timestamp) contra el dispositivo actual y el POS ID.
     */
    fun verifyLicense(context: Context, codigo: String, posId: String): Boolean {
        val parts = codigo.split("|")
        if (parts.size != 2) return false
        val hashIngresado = parts[0]
        val expiracion = parts[1].toLongOrNull() ?: return false

        // Verificar expiración
        if (System.currentTimeMillis() > expiracion) return false

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: return false
        val expectedHash = generarLicencia(deviceId, posId, expiracion).split("|")[0]
        return hashIngresado == expectedHash
    }

    /**
     * Versión sin POS ID (para compatibilidad con código antiguo) - no usar.
     */
    @Deprecated("Usar verifyLicense con posId")
    fun verifyLicense(context: Context, codigo: String): Boolean {
        // Esta versión no funciona sin posId, redirigimos a una versión que pida posId
        // pero no podemos obtener posId aquí, así que lanzamos error o devolvemos false.
        return false
    }

    /**
     * Guarda la licencia y el POS ID en SharedPreferences.
     */
    fun saveLicense(context: Context, codigo: String, posId: String) {
        val parts = codigo.split("|")
        if (parts.size == 2) {
            val expiracion = parts[1].toLongOrNull()
            if (expiracion != null) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_LICENSE, codigo).apply()
                prefs.edit().putLong(KEY_EXPIRATION, expiracion).apply()
                prefs.edit().putString(KEY_POS_ID, posId).apply()
            }
        }
    }

    /**
     * Carga la licencia guardada.
     * @return El código completo de licencia (hash|timestamp) o null
     */
    fun loadLicense(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LICENSE, null)
    }

    /**
     * Carga el POS ID guardado.
     */
    fun loadPosId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_POS_ID, null)
    }

    /**
     * Verifica si hay una licencia válida guardada usando el POS ID almacenado.
     */
    fun hasValidLicense(context: Context): Boolean {
        val license = loadLicense(context) ?: return false
        val posId = loadPosId(context) ?: return false
        return verifyLicense(context, license, posId)
    }

    /**
     * Obtiene el ID del dispositivo actual.
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}