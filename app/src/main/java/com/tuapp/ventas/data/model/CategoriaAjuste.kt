package com.tuapp.ventas.data.model

import java.io.Serializable

/**
 * Categoría persistente de ajuste (gasto o ingreso).
 * Se guarda una sola vez y se reutiliza día tras día.
 */
data class CategoriaAjuste(
    val id: String = System.currentTimeMillis().toString(),
    val nombre: String,
    val tipo: String,              // "GASTO" o "INGRESO"
    val montoSugerido: Double = 0.0  // Monto por defecto al marcar
) : Serializable {
    companion object {
        const val TIPO_GASTO = "GASTO"
        const val TIPO_INGRESO = "INGRESO"
    }
}

/**
 * Ajuste aplicado a un día específico.
 * Vincula una categoría con un monto y estado activo/inactivo.
 */
data class AjusteDia(
    val categoriaId: String,
    val nombre: String,
    val tipo: String,
    val monto: Double,
    val activo: Boolean = true
) : Serializable