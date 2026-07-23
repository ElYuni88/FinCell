package com.tuapp.ventas.ui.estadisticas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.ResumenProducto
import com.tuapp.ventas.databinding.ItemResumenProductoBinding
import com.tuapp.ventas.utils.DateUtils

/** Adaptador para pintar los productos vendidos en el día seleccionado. */
class ResumenAdapter : ListAdapter<ResumenProducto, ResumenAdapter.ResumenViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResumenViewHolder {
        val binding = ItemResumenProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResumenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResumenViewHolder, position: Int) = holder.bind(getItem(position))

    class ResumenViewHolder(private val binding: ItemResumenProductoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ResumenProducto) = with(binding) {
            txtNombreProducto.text = item.nombre
            txtCantidadVendida.text = "Vendidos: ${item.cantidadVendida}"
            txtStockDisponible.text = "Stock: ${item.stockDisponible}"
            txtSubtotal.text = DateUtils.moneda(item.subtotal)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ResumenProducto>() {
        override fun areItemsTheSame(oldItem: ResumenProducto, newItem: ResumenProducto): Boolean = oldItem.nombre == newItem.nombre
        override fun areContentsTheSame(oldItem: ResumenProducto, newItem: ResumenProducto): Boolean = oldItem == newItem
    }
}
