package com.tuapp.ventas.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val fileFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun inicioDia(time: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply { this.timeInMillis = time; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis
    fun finDia(time: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply { this.timeInMillis = inicioDia(time); add(Calendar.DAY_OF_MONTH,1); add(Calendar.MILLISECOND,-1) }.timeInMillis
    fun fechaArchivo(time: Long = System.currentTimeMillis()): String = fileFormat.format(Date(time))
    fun fechaHora(time: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(time))
    fun moneda(valor: Double): String = "$%.2f".format(Locale.US, valor)
}
