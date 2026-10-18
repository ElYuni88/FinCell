package com.tuapp.ventas.utils

/**
 * Utilidades para el manejo de códigos de barras.
 *
 * Reglas:
 * - Normalización: trim + uppercase + sin espacios + límite de 50 caracteres
 * - Válido: al menos un carácter, todos letras/números/guion medio
 * - Solo letras: advertencia (no bloqueante)
 * - Duplicado: bloqueante (no se puede guardar)
 */
object CodigoBarrasUtils {

    const val MAX_LENGTH = 50

    /**
     * Normaliza un código de barras:
     * - Quita espacios al inicio y final
     * - Convierte a MAYÚSCULAS
     * - Elimina TODOS los espacios internos
     * - Trunca a MAX_LENGTH caracteres
     */
    fun normalizar(codigo: String): String {
        return codigo.trim()
            .uppercase()
            .replace(" ", "")
            .take(MAX_LENGTH)
    }

    /**
     * Verifica si el código contiene SOLO letras (sin números).
     * Es una advertencia, no un error.
     */
    fun esSoloLetras(codigo: String): Boolean {
        return codigo.isNotBlank() && codigo.all { it.isLetter() }
    }

    /**
     * Verifica si el código tiene formato válido:
     * - No está vacío
     * - Solo contiene letras, números o guion medio
     * - No excede MAX_LENGTH
     */
    fun tieneFormatoValido(codigo: String): Boolean {
        if (codigo.isBlank()) return false
        if (codigo.length > MAX_LENGTH) return false
        return codigo.all { it.isLetterOrDigit() || it == '-' }
    }
}