package com.tuapp.ventas.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tuapp.ventas.data.model.AjusteDia
import com.tuapp.ventas.data.model.CategoriaAjuste
import com.tuapp.ventas.data.model.Gasto
import com.tuapp.ventas.data.model.ModoOperacion

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("ventas_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Variables existentes (sin cambios)
    var codigoPuntoVenta: String
        get() = prefs.getString("codigo_punto_venta", "") ?: ""
        set(value) = prefs.edit().putString("codigo_punto_venta", value).apply()

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

    // ================================================================
    // ✅ NUEVOS MÉTODOS: CATEGORÍAS PERSISTENTES DE AJUSTES
    // ================================================================

    /** Guarda todas las categorías persistentes (gastos e ingresos). */
    fun guardarCategorias(categorias: List<CategoriaAjuste>) {
        val normalizadas = categorias
            .map { it.copy(nombre = it.nombre.trim(), montoSugerido = it.montoSugerido.coerceAtLeast(0.0)) }
            .filter { it.nombre.isNotBlank() }
        prefs.edit().putString("categorias_ajuste", gson.toJson(normalizadas)).apply()
    }

    /** Recupera todas las categorías persistentes. */
    fun obtenerCategorias(): List<CategoriaAjuste> {
        val json = prefs.getString("categorias_ajuste", null) ?: return emptyList()
        return runCatching {
            val tipo = object : TypeToken<List<CategoriaAjuste>>() {}.type
            gson.fromJson<List<CategoriaAjuste>>(json, tipo).orEmpty()
        }.getOrDefault(emptyList())
    }

    /** Agrega una nueva categoría persistente. */
    fun agregarCategoria(categoria: CategoriaAjuste) {
        val actuales = obtenerCategorias().toMutableList()
        actuales.add(categoria)
        guardarCategorias(actuales)
    }

    /** Elimina una categoría persistente por ID. */
    fun eliminarCategoria(id: String) {
        val actuales = obtenerCategorias().filterNot { it.id == id }
        guardarCategorias(actuales)
    }

    // ================================================================
    // ✅ NUEVOS MÉTODOS: AJUSTES DEL DÍA
    // ================================================================

    /** Guarda los ajustes aplicados a un día específico. */
    fun guardarAjustesDia(fecha: String, ajustes: List<AjusteDia>) {
        val json = gson.toJson(ajustes)
        prefs.edit().putString("ajustes_dia_$fecha", json).apply()
    }

    /** Recupera los ajustes de un día específico. */
    fun obtenerAjustesDia(fecha: String): List<AjusteDia> {
        val json = prefs.getString("ajustes_dia_$fecha", null) ?: return emptyList()
        return runCatching {
            val tipo = object : TypeToken<List<AjusteDia>>() {}.type
            gson.fromJson<List<AjusteDia>>(json, tipo).orEmpty()
        }.getOrDefault(emptyList())
    }

    /** Obtiene solo los gastos activos del día. */
    fun obtenerGastosActivosDia(fecha: String): List<AjusteDia> =
        obtenerAjustesDia(fecha).filter { it.tipo == CategoriaAjuste.TIPO_GASTO && it.activo }

    /** Obtiene solo los ingresos activos del día. */
    fun obtenerIngresosActivosDia(fecha: String): List<AjusteDia> =
        obtenerAjustesDia(fecha).filter { it.tipo == CategoriaAjuste.TIPO_INGRESO && it.activo }

    // ================================================================
    // ⚠️ MÉTODOS DEPRECATED (mantener por compatibilidad)
    // ================================================================

    @Deprecated("Usar guardarAjustesDia en su lugar")
    fun guardarGastos(fecha: String, gastos: List<Gasto>) {
        // Convertir Gasto → AjusteDia
        val ajustes = gastos.map {
            AjusteDia(
                categoriaId = it.categoria,
                nombre = it.categoria,
                tipo = CategoriaAjuste.TIPO_GASTO,
                monto = it.monto,
                activo = true
            )
        }
        // Preservar los ingresos existentes
        val ingresosExistentes = obtenerAjustesDia(fecha).filter { it.tipo == CategoriaAjuste.TIPO_INGRESO }
        guardarAjustesDia(fecha, ajustes + ingresosExistentes)
    }

    @Deprecated("Usar obtenerGastosActivosDia en su lugar")
    fun obtenerGastos(fecha: String): List<Gasto> {
        return obtenerGastosActivosDia(fecha).map {
            Gasto(categoria = it.nombre, monto = it.monto)
        }
    }

    /** @deprecated Mantener compatibilidad con código antiguo */
    @Deprecated("Usar guardarCategorias en su lugar")
    fun guardarGastos(gastos: List<Gasto>) {
        val categorias = gastos.map {
            CategoriaAjuste(
                id = it.categoria,
                nombre = it.categoria,
                tipo = CategoriaAjuste.TIPO_GASTO,
                montoSugerido = it.monto
            )
        }
        // Preservar ingresos existentes
        val ingresosExistentes = obtenerCategorias().filter { it.tipo == CategoriaAjuste.TIPO_INGRESO }
        guardarCategorias(categorias + ingresosExistentes)
    }

    /** @deprecated Usar obtenerCategorias() en su lugar */
    fun obtenerGastos(): List<Gasto> {
        return obtenerCategorias()
            .filter { it.tipo == CategoriaAjuste.TIPO_GASTO }
            .map { Gasto(categoria = it.nombre, monto = it.montoSugerido) }
    }

    // ================================================================
// ✅ CONTROL DE EXPORTACIÓN DE IPB
// ================================================================

    /**
     * Guarda la fecha (timestamp) del último IPB exportado exitosamente.
     */
    fun guardarUltimaExportacionIPB(timestamp: Long) {
        prefs.edit().putLong("ultima_exportacion_ipb", timestamp).apply()
    }

    /**
     * Obtiene la fecha del último IPB exportado.
     * Devuelve 0 si nunca se ha exportado.
     */
    fun obtenerUltimaExportacionIPB(): Long {
        return prefs.getLong("ultima_exportacion_ipb", 0L)
    }

    /**
     * Verifica si ha pasado más de 1 día desde la última exportación.
     */
    fun necesitaExportarIPB(): Boolean {
        val ultima = obtenerUltimaExportacionIPB()
        if (ultima == 0L) return true  // Nunca ha exportado
        val unDiaMs = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - ultima) > unDiaMs
    }

    // Mantenemos la clave privada por si acaso (ya no se usa)
    @Suppress("unused")
    private fun claveGastos(fecha: String): String = "gastos_ipb_$fecha"
}