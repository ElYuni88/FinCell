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

    // ================================================================
// ✅ PERIODO MUERTO (entre exportación e importación del IPV)
// ================================================================

    /**
     * Guarda el timestamp del inicio del periodo muerto.
     * Se llama al exportar exitosamente el IPB.
     * Si ya había un periodo muerto activo, se sobrescribe (opción A).
     */
    fun guardarInicioPeriodoMuerto(timestamp: Long) {
        prefs.edit().putLong("periodo_muerto_inicio", timestamp).apply()
    }

    /**
     * Obtiene el timestamp del inicio del periodo muerto.
     * Devuelve 0 si no hay periodo muerto activo.
     */
    fun obtenerInicioPeriodoMuerto(): Long {
        return prefs.getLong("periodo_muerto_inicio", 0L)
    }

    /**
     * Limpia el periodo muerto (se llama al importar el IPV).
     */
    fun limpiarInicioPeriodoMuerto() {
        prefs.edit().remove("periodo_muerto_inicio").apply()
    }

    /**
     * Verifica si hay un periodo muerto activo.
     */
    fun estaEnPeriodoMuerto(): Boolean {
        return estadoPeriodo == com.tuapp.ventas.data.model.EstadoPeriodo.CERRADO
    }

    // Mantenemos la clave privada por si acaso (ya no se usa)
    @Suppress("unused")
    private fun claveGastos(fecha: String): String = "gastos_ipb_$fecha"


    // ================================================================
// ✅ CONTROL DE NOTIFICACIONES (una vez al día)
// ================================================================

    private val unDiaMs = 24 * 60 * 60 * 1000L

    /**
     * Verifica si ha pasado al menos 24h desde la última vez que se generó
     * una notificación de este tipo.
     * Si nunca se generó, devuelve true.
     */
    fun puedeGenerarNotificacion(tipo: String): Boolean {
        val ultimaVez = prefs.getLong("notif_last_$tipo", 0L)
        if (ultimaVez == 0L) return true
        return (System.currentTimeMillis() - ultimaVez) >= unDiaMs
    }

    /**
     * Guarda el timestamp actual como "última generación" de este tipo.
     */
    fun marcarNotificacionGenerada(tipo: String) {
        prefs.edit().putLong("notif_last_$tipo", System.currentTimeMillis()).apply()
    }

    /**
     * Limpia el timestamp de una notificación para que la próxima vez
     * que se cumpla la condición se genere de inmediato.
     */
    fun limpiarTimestampNotificacion(tipo: String) {
        prefs.edit().remove("notif_last_$tipo").apply()
    }


    // ================================================================
// ✅ ESTADO DEL PERIODO DE VENTAS
// ================================================================

    /**
     * Estado actual del periodo de ventas.
     * Por defecto es ABIERTO.
     */
    var estadoPeriodo: com.tuapp.ventas.data.model.EstadoPeriodo
        get() = com.tuapp.ventas.data.model.EstadoPeriodo.fromNombre(
            prefs.getString("estado_periodo", null)
        )
        set(value) = prefs.edit().putString("estado_periodo", value.name).apply()

    /**
     * Fecha/hora en que se cerró el último periodo.
     * Coincide con el inicio del periodo muerto.
     * 0 si nunca se ha cerrado.
     */
    var fechaCierrePeriodo: Long
        get() = prefs.getLong("fecha_cierre_periodo", 0L)
        set(value) = prefs.edit().putLong("fecha_cierre_periodo", value).apply()

    /**
     * Nombre del archivo IPB exportado al cerrar el periodo.
     * Sirve para intentar borrarlo al revertir.
     * Vacío si no hay.
     */
    var nombreArchivoExportado: String
        get() = prefs.getString("nombre_archivo_exportado", "") ?: ""
        set(value) = prefs.edit().putString("nombre_archivo_exportado", value).apply()

    /**
     * Limpia todo lo relacionado con el estado del periodo:
     * - Estado → ABIERTO
     * - Fecha de cierre → 0
     * - Nombre del archivo exportado → ""
     * - Marca de periodo muerto → eliminada
     */
    fun limpiarEstadoPeriodo() {
        prefs.edit().apply {
            putString("estado_periodo", com.tuapp.ventas.data.model.EstadoPeriodo.ABIERTO.name)
            remove("fecha_cierre_periodo")
            remove("nombre_archivo_exportado")
            remove("periodo_muerto_inicio")
            apply()
        }
    }

    /**
     * Verifica si una clave existe en SharedPreferences.
     * Útil para detectar "primera vez".
     */
    fun tieneClave(clave: String): Boolean = prefs.contains(clave)

    // ================================================================
// ✅ RESETEO DE LA APP
// ================================================================

    /**
     * Limpia SOLO las preferencias relacionadas con historial de uso.
     * Conserva:
     * - codigo_punto_venta
     * - modo_actual, modo_default
     * - sonido, vibrar, confirmar_cuenta, backup_auto
     * - categorias_ajuste
     *
     * Borra:
     * - cuenta_seleccionada
     * - tooltip_modo
     * - ajustes_dia_* (todas)
     * - gastos_ipb_* (legacy)
     * - ultima_exportacion_ipb
     */
    fun limpiarParaReset() {
        val editor = prefs.edit()

        // Borrar claves directas
        editor.remove("cuenta_seleccionada")
        editor.remove("tooltip_modo")
        editor.remove("ultima_exportacion_ipb")
        editor.remove("periodo_muerto_inicio")
        // ✅ NUEVO: limpiar estado del periodo
        editor.remove("estado_periodo")
        editor.remove("fecha_cierre_periodo")
        editor.remove("nombre_archivo_exportado")

        // Borrar ajustes por día (prefijo "ajustes_dia_")
        prefs.all.keys
            .filter { it.startsWith("ajustes_dia_") || it.startsWith("gastos_ipb_") }
            .forEach { editor.remove(it)
            }

        editor.apply()
    }
}