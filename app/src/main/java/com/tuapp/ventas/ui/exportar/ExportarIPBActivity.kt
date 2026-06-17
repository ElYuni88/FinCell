package com.tuapp.ventas.ui.exportar

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.ArchivoIPB
import com.tuapp.ventas.data.model.ProductoIPB
import com.tuapp.ventas.data.model.ResumenIPB
import com.tuapp.ventas.utils.DateUtils
import kotlinx.coroutines.launch
import java.io.File

class ExportarIPBActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); exportar() }
    private fun exportar() = lifecycleScope.launch {
        runCatching {
            val repo=(application as VentasApplication).repository; val now=System.currentTimeMillis(); val inicio=DateUtils.inicioDia(now); val fin=DateUtils.finDia(now)
            val productos=repo.listarProductos().map{ProductoIPB(it.id,it.nombre,it.codigoBarras,it.precio,it.inventario,it.vendidos)}
            val ventas=repo.ventasDirectasDia(inicio,fin); val cuentas=repo.cuentasCerradasDia(inicio,fin)
            val archivo=ArchivoIPB(DateUtils.fechaArchivo(now), now, productos, ResumenIPB(ventas.sumOf{it.precio}, cuentas.sumOf{it.cuenta.total}, ventas.sumOf{it.precio}+cuentas.sumOf{it.cuenta.total}, ventas.size, cuentas.size))
            guardar("ipb_${archivo.fecha}.json", GsonBuilder().setPrettyPrinting().create().toJson(archivo))
        }.onSuccess{ Toast.makeText(this,"IPB exportado en Descargas",Toast.LENGTH_LONG).show(); finish() }.onFailure{ Toast.makeText(this,it.message,Toast.LENGTH_LONG).show(); finish() }
    }
    private fun guardar(nombre: String, json: String) { if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){ val values=ContentValues().apply{put(MediaStore.MediaColumns.DISPLAY_NAME,nombre); put(MediaStore.MediaColumns.MIME_TYPE,"application/json"); put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS)}; val uri=contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)?:error("No se pudo crear IPB"); contentResolver.openOutputStream(uri).use{it?.write(json.toByteArray())?:error("No se pudo escribir IPB")} } else { val dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); if(!dir.exists())dir.mkdirs(); File(dir,nombre).writeText(json) } }
}
