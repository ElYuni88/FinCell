package com.tuapp.ventas.ui.productosmanuales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.VentaItem
import com.tuapp.ventas.databinding.ItemVentaTemporalBinding
import com.tuapp.ventas.utils.DateUtils

class VentaTemporalAdapter(
    private val onEliminar: (VentaItem) -> Unit
) : ListAdapter<VentaItem, VentaTemporalAdapter.ViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<VentaItem>() {
        override fun areItemsTheSame(oldItem: VentaItem, newItem: VentaItem) =
            oldItem.producto.id == newItem.producto.id

        override fun areContentsTheSame(oldItem: VentaItem, newItem: VentaItem) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVentaTemporalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemVentaTemporalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VentaItem) = with(binding) {
            tvNombre.text = item.producto.nombre
            tvCantidad.text = "x${item.cantidad}"
            tvSubtotal.text = DateUtils.moneda(item.subtotal)
            btnEliminar.setOnClickListener { onEliminar(item) }
        }
    }
}