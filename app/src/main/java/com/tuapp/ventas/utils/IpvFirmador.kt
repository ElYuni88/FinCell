package com.tuapp.ventas.utils

import com.google.gson.Gson
import com.tuapp.ventas.BuildConfig
import com.google.gson.GsonBuilder
import com.tuapp.ventas.data.model.ProductoIPV
import java.security.MessageDigest
import java.util.Locale

/**
 * Firma y verificación del Archivo IPV.
 *
 * Cadena de firma (igual que en Admin):
 *   "IPV|$codigoPv|$fechaExportacion|$productosJsonMin|$SECRET_IPV"
 *
 * ⚠️ El `productosJsonMin` es el JSON generado por Gson sobre List<ProductoIPV>.
 *    Como los campos tienen @SerializedName, el JSON es idéntico en Admin y Cliente.
 */
object IpvFirmador {

    private val SECRET_IPV = BuildConfig.SECRET_IPV
    private val gsonCompacto: Gson = GsonBuilder().create()

    fun firmar(
        codigoPv: String,
        fechaExportacion: Long,
        productos: List<ProductoIPV>
    ): String {
        val productosJson = gsonCompacto.toJson(productos)
        val cadena = "IPV|$codigoPv|$fechaExportacion|$productosJson|$SECRET_IPV"
        return sha256(cadena)
    }

    fun verificar(
        codigoPv: String,
        fechaExportacion: Long,
        productos: List<ProductoIPV>,
        firmaEsperada: String
    ): Boolean {
        val calculada = firmar(codigoPv, fechaExportacion, productos)
        return calculada == firmaEsperada
    }

    private fun sha256(texto: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(texto.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(Locale.US, it) }
    }
}