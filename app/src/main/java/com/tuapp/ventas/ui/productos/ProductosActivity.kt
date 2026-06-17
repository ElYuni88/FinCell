package com.tuapp.ventas.ui.productos

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ActivityProductosBinding

class ProductosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductosBinding
    private val viewModel: ProductosViewModel by viewModels { ProductosViewModelFactory((application as VentasApplication).repository) }
    private val adapter = ProductoAdapter(::mostrarEditor, ::confirmarEliminar)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); binding=ActivityProductosBinding.inflate(layoutInflater); setContentView(binding.root); binding.recyclerProductos.layoutManager=LinearLayoutManager(this); binding.recyclerProductos.adapter=adapter; binding.btnAgregarProducto.setOnClickListener{mostrarEditor(null)}; binding.btnVolver.setOnClickListener{finish()}; viewModel.productos.observe(this){adapter.submitList(it)}; viewModel.mensaje.observe(this){Toast.makeText(this,it,Toast.LENGTH_SHORT).show()} }
    private fun mostrarEditor(producto: Producto?) {
        val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(48,16,48,0)}
        val nombre=EditText(this).apply{hint="Nombre"; setText(producto?.nombre.orEmpty())}
        val codigo=EditText(this).apply{hint="Código de barras o manual"; setText(producto?.codigoBarras.orEmpty()); isEnabled=producto?.codigoBarras.isNullOrBlank()}
        val precio=EditText(this).apply{hint="Precio"; inputType=8194; setText(producto?.precio?.toString().orEmpty())}
        val inventario=EditText(this).apply{hint="Inventario"; inputType=2; setText((producto?.inventario ?: 0).toString())}
        layout.addView(nombre); layout.addView(codigo); layout.addView(precio); layout.addView(inventario)
        MaterialAlertDialogBuilder(this).setTitle(if(producto==null)"Agregar producto" else "Editar producto").setView(layout).setNegativeButton("Cancelar",null).setPositiveButton("Guardar"){_,_->
            val p=precio.text.toString().toDoubleOrNull(); if(nombre.text.isBlank()||p==null){ Toast.makeText(this,"Nombre y precio válidos requeridos",Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            val tipo=if(codigo.text.isBlank()) Producto.TIPO_MANUAL else producto?.tipoProducto ?: Producto.TIPO_CODIGO_BARRAS
            viewModel.guardar((producto ?: Producto(nombre = "", precio = 0.0)).copy(nombre=nombre.text.toString(), codigoBarras=codigo.text.toString().trim(), precio=p, inventario=inventario.text.toString().toIntOrNull()?:0, tipoProducto=tipo))
        }.show()
    }
    private fun confirmarEliminar(producto: Producto)=MaterialAlertDialogBuilder(this).setTitle("Eliminar producto").setMessage("¿Eliminar ${producto.nombre}?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar"){_,_->viewModel.eliminar(producto)}.show()
}
