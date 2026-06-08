package com.tuapp.ventas.ui.cuenta.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.relaciones.DetalleConProducto
import com.tuapp.ventas.databinding.ItemDetalleCuentaBinding
import com.tuapp.ventas.utils.DateUtils

class DetalleCuentaAdapter : ListAdapter<DetalleConProducto, DetalleCuentaAdapter.VH>(Diff) {
    object Diff : DiffUtil.ItemCallback<DetalleConProducto>() { override fun areItemsTheSame(o: DetalleConProducto, n: DetalleConProducto) = o.detalle.id == n.detalle.id; override fun areContentsTheSame(o: DetalleConProducto, n: DetalleConProducto) = o == n }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemDetalleCuentaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    class VH(private val binding: ItemDetalleCuentaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DetalleConProducto) { binding.txtProducto.text = item.producto.nombre; binding.txtCantidad.text = "x${item.detalle.cantidad}"; binding.txtSubtotal.text = DateUtils.moneda(item.detalle.subtotal) }
    }
}
