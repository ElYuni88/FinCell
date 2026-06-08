package com.tuapp.ventas.utils

object CsvExporter { fun escapar(valor: String): String = "\"${valor.replace("\"", "\"\"")}\"" }
