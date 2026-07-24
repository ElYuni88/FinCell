package com.tuapp.ventas.ui.estadisticas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.ExistenciaProducto
import com.tuapp.ventas.databinding.ItemExistenciaProductoBinding
import com.tuapp.ventas.utils.DateUtils

/** Adaptador para mostrar la existencia actual de todos los productos. */
class ExistenciaAdapter : ListAdapter<ExistenciaProducto, ExistenciaAdapter.ExistenciaViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExistenciaViewHolder {
        val binding = ItemExistenciaProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExistenciaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExistenciaViewHolder, position: Int) = holder.bind(getItem(position))

    class ExistenciaViewHolder(private val binding: ItemExistenciaProductoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExistenciaProducto) = with(binding) {
            txtNombreProducto.text = item.nombre
            txtStockDisponible.text = "Stock: ${item.stockDisponible}"
            txtPrecio.text = DateUtils.moneda(item.precio)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ExistenciaProducto>() {
        override fun areItemsTheSame(oldItem: ExistenciaProducto, newItem: ExistenciaProducto): Boolean = oldItem.nombre == newItem.nombre
        override fun areContentsTheSame(oldItem: ExistenciaProducto, newItem: ExistenciaProducto): Boolean = oldItem == newItem
    }
}
