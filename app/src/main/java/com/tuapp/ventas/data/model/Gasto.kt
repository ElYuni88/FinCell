package com.tuapp.ventas.data.model

import java.io.Serializable

/**
 * Representa un gasto dinámico del día para el informe IPB.
 * Se marca como Serializable para permitir su almacenamiento simple en preferencias.
 */
data class Gasto(
    val categoria: String,
    val monto: Double
) : Serializable
