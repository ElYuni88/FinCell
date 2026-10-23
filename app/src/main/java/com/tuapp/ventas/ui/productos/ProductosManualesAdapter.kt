package com.tuapp.ventas.ui.productosmanuales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.R
import com.tuapp.ventas.databinding.ItemProductoManualBinding
import com.tuapp.ventas.utils.DateUtils

class ProductosManualesAdapter(
    private val onProductoClick: (ProductoConStockAjustado) -> Unit
) : ListAdapter<ProductoConStockAjustado, ProductosManualesAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<ProductoConStockAjustado>() {
        override fun areItemsTheSame(oldItem: ProductoConStockAjustado, newItem: ProductoConStockAjustado) =
            oldItem.producto.id == newItem.producto.id

        override fun areContentsTheSame(oldItem: ProductoConStockAjustado, newItem: ProductoConStockAjustado) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemProductoManualBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemProductoManualBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductoConStockAjustado) = with(binding) {
            val producto = item.producto
            val ctx = root.context

            tvNombre.text = producto.nombre
            tvPrecio.text = DateUtils.moneda(producto.precio)
            tvCodigo.text = "Código: ${producto.codigoBarras.ifBlank { "S/C" }}"

            if (item.sinStock) {
                // ✅ Sin stock: gris, no clickeable
                tvStock.text = "SIN STOCK"
                tvStock.setTextColor(ContextCompat.getColor(ctx, R.color.colorError))
                root.alpha = 0.5f
                root.isEnabled = false
                root.setOnClickListener(null)
            } else {
                // ✅ Con stock: normal, clickeable
                tvStock.text = "Stock: ${item.stockDisponible}"
                tvStock.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                root.alpha = 1f
                root.isEnabled = true
                root.setOnClickListener { onProductoClick(item) }
            }
        }
    }
}