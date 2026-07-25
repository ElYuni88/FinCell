package com.tuapp.ventas.ui.ipb

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.ProductoIPB
import com.tuapp.ventas.databinding.ItemIpbProductoBinding
import com.tuapp.ventas.utils.DateUtils

class IPBAdapter(private val productos: List<ProductoIPB>) :
    RecyclerView.Adapter<IPBAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemIpbProductoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIpbProductoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = productos[position]
        with(holder.binding) {
            tvNombre.text = item.nombre
            tvCodigo.text = item.codigoBarras?.takeIf { it.isNotBlank() } ?: "S/C"
            tvPrecio.text = DateUtils.moneda(item.precio)
            tvInventario.text = item.inventario.toString()
            tvVendidos.text = item.vendidos.toString()
        }
    }

    override fun getItemCount() = productos.size
}