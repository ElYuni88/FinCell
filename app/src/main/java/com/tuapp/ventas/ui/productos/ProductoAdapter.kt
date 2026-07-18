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
            val codigo = producto.codigoBarras.ifBlank { "S/C" }
            val etiquetaCodigo = if (producto.tipoProducto == Producto.TIPO_MANUAL) "$codigo (manual)" else codigo
            val stockDisponible = producto.inventario - producto.vendidos
            txtNombre.text = producto.nombre
            txtDetalle.text = "Código: $etiquetaCodigo · Precio: ${DateUtils.moneda(producto.precio)} · Stock: $stockDisponible · Vendidos: ${producto.vendidos}"
            btnEditar.setOnClickListener { onEditar(producto) }
            btnEliminar.setOnClickListener { onEliminar(producto) }
        }
    }
}
