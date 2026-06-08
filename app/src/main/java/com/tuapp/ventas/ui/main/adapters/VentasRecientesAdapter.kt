package com.tuapp.ventas.ui.main.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.VentaDirecta
import com.tuapp.ventas.databinding.ItemVentaRecienteBinding
import com.tuapp.ventas.utils.DateUtils

class VentasRecientesAdapter : ListAdapter<VentaDirecta, VentasRecientesAdapter.VH>(Diff) {
    object Diff : DiffUtil.ItemCallback<VentaDirecta>() { override fun areItemsTheSame(o: VentaDirecta, n: VentaDirecta) = o.id == n.id; override fun areContentsTheSame(o: VentaDirecta, n: VentaDirecta) = o == n }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemVentaRecienteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    class VH(private val binding: ItemVentaRecienteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(venta: VentaDirecta) { binding.txtProducto.text = venta.nombreProducto; binding.txtPrecio.text = DateUtils.moneda(venta.precio); binding.txtFecha.text = DateUtils.fechaHora(venta.fechaVenta) }
    }
}
