package com.tuapp.ventas.utils

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.tuapp.ventas.data.model.ArchivoIPV
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Lee archivos .ipv (GZIP + JSON) exportados por el Admin.
 */
object IpvReader {

    fun leerDesdeUri(context: Context, uri: Uri): ArchivoIPV {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("No se pudo abrir el archivo")

        return inputStream.use { stream ->
            val buffered = BufferedInputStream(stream)
            buffered.mark(2)
            val primero = buffered.read()
            val segundo = buffered.read()
            buffered.reset()

            val esGzip = primero == 0x1f && segundo == 0x8b

            val json = if (esGzip) {
                descomprimirGzip(buffered)
            } else {
                buffered.bufferedReader().use { it.readText() }
            }

            try {
                Gson().fromJson(json, ArchivoIPV::class.java)
                    ?: error("El archivo IPV no contiene datos")
            } catch (e: JsonSyntaxException) {
                error("El JSON del IPV no es válido: ${e.message}")
            }
        }
    }

    private fun descomprimirGzip(inputStream: InputStream): String {
        GZIPInputStream(inputStream).use { gzip ->
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var leidos: Int
            while (gzip.read(buffer).also { leidos = it } != -1) {
                baos.write(buffer, 0, leidos)
            }
            return baos.toString(Charsets.UTF_8.name())
        }
    }
}