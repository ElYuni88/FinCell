package com.tuapp.ventas.data.model

data class VerificacionResult(val valido: Boolean, val mensaje: String, val hashCalculado: String? = null, val hashArchivo: String? = null)
