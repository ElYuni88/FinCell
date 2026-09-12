package com.tuapp.ventas.utils

import java.text.Normalizer

/**
 * Utilidades de texto para normalización.
 *
 * Permite buscar ignorando tildes: "azucar" encuentra "Azúcar".
 */
object TextUtils {

    /**
     * Normaliza un texto quitando tildes, diacríticos y convirtiendo a minúsculas.
     *
     * Ejemplos:
     * - "Azúcar" → "azucar"
     * - "Café Americano" → "cafe americano"
     * - "JOSÉ" → "jose"
     * - "Ñoño" → "nono"
     */
    fun normalizar(texto: String): String {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase()
    }

    /**
     * Verifica si un texto contiene una búsqueda, ignorando tildes y mayúsculas.
     *
     * Ejemplo: contiene("Azúcar", "azucar") → true
     */
    fun contiene(texto: String, busqueda: String): Boolean {
        return normalizar(texto).contains(normalizar(busqueda))
    }
}