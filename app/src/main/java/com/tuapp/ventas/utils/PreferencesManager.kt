package com.tuapp.ventas.utils

import android.content.Context
import com.tuapp.ventas.data.model.ModoOperacion

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("ventas_prefs", Context.MODE_PRIVATE)
    var modoActual: ModoOperacion
        get() = ModoOperacion.valueOf(prefs.getString("modo_actual", ModoOperacion.SIMPLE.name) ?: ModoOperacion.SIMPLE.name)
        set(value) = prefs.edit().putString("modo_actual", value.name).apply()
    var cuentaSeleccionadaId: Long
        get() = prefs.getLong("cuenta_seleccionada", -1L)
        set(value) = prefs.edit().putLong("cuenta_seleccionada", value).apply()
    var tooltipModoMostrado: Boolean
        get() = prefs.getBoolean("tooltip_modo", false)
        set(value) = prefs.edit().putBoolean("tooltip_modo", value).apply()
    var sonidoEscaneo: Boolean
        get() = prefs.getBoolean("sonido", true)
        set(value) = prefs.edit().putBoolean("sonido", value).apply()
    var vibrarEscaneo: Boolean
        get() = prefs.getBoolean("vibrar", true)
        set(value) = prefs.edit().putBoolean("vibrar", value).apply()
    var confirmarCuenta: Boolean
        get() = prefs.getBoolean("confirmar_cuenta", true)
        set(value) = prefs.edit().putBoolean("confirmar_cuenta", value).apply()
    var backupAutomatico: Boolean
        get() = prefs.getBoolean("backup_auto", false)
        set(value) = prefs.edit().putBoolean("backup_auto", value).apply()
    var modoPredeterminado: String
        get() = prefs.getString("modo_default", "RECORDAR") ?: "RECORDAR"
        set(value) = prefs.edit().putString("modo_default", value).apply()
}
