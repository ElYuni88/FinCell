package com.tuapp.ventas.data.model

/**
 * Estado del periodo de ventas.
 *
 * - ABIERTO: se puede vender, exportar y cerrar.
 * - CERRADO: periodo muerto activo. Se puede vender (las ventas van al periodo muerto),
 *   pero NO se puede exportar. Solo se reabre al importar el IPV del admin
 *   o al revertir el cierre desde Ajustes.
 */
enum class EstadoPeriodo {
    ABIERTO,
    CERRADO;

    companion object {
        fun fromNombre(nombre: String?): EstadoPeriodo =
            entries.firstOrNull { it.name == nombre } ?: ABIERTO
    }
}