package com.tuapp.ventas.ui.venta

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.VentaItem
import com.tuapp.ventas.databinding.ItemVentaMultipleBinding
import com.tuapp.ventas.utils.DateUtils

class VentaMultipleAdapter(
    private val onEliminar: (VentaItem) -> Unit
) : ListAdapter<VentaItem, VentaMultipleAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<VentaItem>() {
        override fun areItemsTheSame(oldItem: VentaItem, newItem: VentaItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: VentaItem, newItem: VentaItem) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemVentaMultipleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemVentaMultipleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VentaItem) = with(binding) {
            tvNombre.text = item.producto.nombre
            tvCantidad.text = "Cantidad: ${item.cantidad}"
            tvSubtotal.text = DateUtils.moneda(item.subtotal)
            btnEliminar.setOnClickListener { onEliminar(item) }
        }
    }
}
