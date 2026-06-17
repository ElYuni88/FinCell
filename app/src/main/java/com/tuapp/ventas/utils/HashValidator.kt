package com.tuapp.ventas.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tuapp.ventas.data.model.ArchivoVentas
import com.tuapp.ventas.data.model.VerificacionResult
import java.io.File
import java.security.MessageDigest

object HashValidator {
    private const val SALT_GLOBAL = "MiCafe2026_Salt_Seguro_NoCompartir"
    private val gson: Gson = GsonBuilder().create()

    fun sha256(texto: String): String = MessageDigest.getInstance("SHA-256")
        .digest(texto.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun hashVentaSimple(id: Long, codigo: String, precio: Double, fechaVenta: Long): String = sha256("$id$codigo$precio$fechaVenta$SALT_GLOBAL")
    fun hashCuenta(cuentaId: Long, total: Double, fechaCierre: Long): String = sha256("$cuentaId$total$fechaCierre$SALT_GLOBAL")
    fun hashTotal(hashes: List<String>, hashAnterior: String?, fecha: String): String = sha256(hashes.joinToString("") + (hashAnterior ?: "") + fecha)

    fun verificarArchivoVentas(rutaArchivo: String): VerificacionResult = runCatching {
        val archivo = gson.fromJson(File(rutaArchivo).readText(), ArchivoVentas::class.java)
        val hashes = mutableListOf<String>()
        archivo.detalleVentasSimples.forEach {
            val calculado = hashVentaSimple(it.id, it.codigoBarras, it.precio, it.fechaVenta)
            if (calculado != it.hashLinea) return VerificacionResult(false, "Hash inválido en venta simple ${it.id}", calculado, it.hashLinea)
            hashes += calculado
        }
        archivo.detalleCuentas.forEach {
            val calculado = hashCuenta(it.cuentaId, it.total, it.fechaCierre)
            if (calculado != it.hashLinea) return VerificacionResult(false, "Hash inválido en cuenta ${it.cuentaId}", calculado, it.hashLinea)
            hashes += calculado
        }
        val total = hashTotal(hashes, archivo.hashAnterior, archivo.fecha)
        if (total == archivo.hashTotal) VerificacionResult(true, "Archivo íntegro", total, archivo.hashTotal)
        else VerificacionResult(false, "Hash total inválido", total, archivo.hashTotal)
    }.getOrElse { VerificacionResult(false, it.message ?: "No se pudo verificar el archivo") }
}
