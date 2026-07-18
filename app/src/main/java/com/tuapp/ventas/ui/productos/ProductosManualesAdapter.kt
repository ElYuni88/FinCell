package com.tuapp.ventas.ui.productosmanuales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ItemProductoManualBinding
import com.tuapp.ventas.utils.DateUtils

class ProductosManualesAdapter(
    private val onProductoClick: (Producto) -> Unit
) : ListAdapter<Producto, ProductosManualesAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(oldItem: Producto, newItem: Producto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Producto, newItem: Producto) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemProductoManualBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemProductoManualBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Producto) = with(binding) {
            val stockDisponible = producto.inventario - producto.vendidos
            tvNombre.text = producto.nombre
            tvPrecio.text = DateUtils.moneda(producto.precio)
            tvStock.text = "Stock: $stockDisponible"
            tvCodigo.text = "Código: ${producto.codigoBarras.ifBlank { "S/C" }}"
            root.setOnClickListener { onProductoClick(producto) }
        }
    }
}