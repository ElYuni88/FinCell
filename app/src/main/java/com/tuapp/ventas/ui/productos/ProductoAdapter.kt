package com.tuapp.ventas.ui.productos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ItemProductoBinding
import com.tuapp.ventas.utils.DateUtils

class ProductoAdapter(
    private val onEditar: (Producto) -> Unit,
    private val onEliminar: (Producto) -> Unit
) : ListAdapter<Producto, ProductoAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(oldItem: Producto, newItem: Producto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Producto, newItem: Producto) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemProductoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Producto) = with(binding) {
            val codigo = if (producto.tipoProducto == Producto.TIPO_MANUAL) {
                "${producto.codigoBarras} (manual)"
            } else {
                producto.codigoBarras
            }
            txtNombre.text = producto.nombre
            txtDetalle.text = "Código: $codigo · ${DateUtils.moneda(producto.precio)} · Inventario: ${producto.inventario} · Vendidos: ${producto.vendidos}"
            btnEditar.setOnClickListener { onEditar(producto) }
            btnEliminar.setOnClickListener { onEliminar(producto) }
        }
    }
}
