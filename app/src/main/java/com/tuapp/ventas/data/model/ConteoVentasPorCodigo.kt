package com.tuapp.ventas.data.model

/**
 * Resultado de consulta SQL: cantidad de ventas agrupadas por código de barras.
 * Se usa para calcular las ventas del "periodo muerto".
 */
data class ConteoVentasPorCodigo(
    val codigoBarras: String,
    val cantidad: Int
)