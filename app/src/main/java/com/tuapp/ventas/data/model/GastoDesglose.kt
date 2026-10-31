package com.tuapp.ventas.data.model

/** Un gasto individual para el desglose en la pantalla de análisis. */
data class GastoDesglose(
    val nombre: String,
    val monto: Double
)