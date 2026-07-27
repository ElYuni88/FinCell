package com.tuapp.ventas.ui.ipb

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.ProductoIPB
import com.tuapp.ventas.databinding.ItemIpbProductoBinding
import com.tuapp.ventas.utils.DateUtils

/** Adaptador de la tabla de productos del informe IPB. */
class IPBAdapter(private var productos: List<ProductoIPB>) :
    RecyclerView.Adapter<IPBAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemIpbProductoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIpbProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = productos[position]
        with(holder.binding) {
            tvNombre.text = item.nombre
            tvVendidos.text = item.vendidos.toString()
            tvStock.text = item.stockFinal.toString()
            tvSubtotal.text = DateUtils.moneda(item.subtotal)
        }
    }

    override fun getItemCount() = productos.size

    fun submitList(nuevosProductos: List<ProductoIPB>) {
        productos = nuevosProductos
        notifyDataSetChanged()
    }
}
