package com.tuapp.ventas.ui.productos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.ItemProductoBinding
import com.tuapp.ventas.utils.DateUtils

class ProductoAdapter(private val onEditar: (Producto) -> Unit, private val onEliminar: (Producto) -> Unit) : ListAdapter<Producto, ProductoAdapter.VH>(Diff) {
    object Diff : DiffUtil.ItemCallback<Producto>() { override fun areItemsTheSame(o: Producto, n: Producto)=o.id==n.id; override fun areContentsTheSame(o: Producto, n: Producto)=o==n }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)=VH(ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int)=holder.bind(getItem(position))
    inner class VH(private val b: ItemProductoBinding): RecyclerView.ViewHolder(b.root){ fun bind(p: Producto){ b.txtNombre.text=p.nombre; b.txtDetalle.text="Código: ${p.codigoBarras ?: "S/C"} · ${DateUtils.moneda(p.precio)} · Inventario: ${p.inventario} · Vendidos: ${p.vendidos}"; b.btnEditar.setOnClickListener{onEditar(p)}; b.btnEliminar.setOnClickListener{onEliminar(p)} } }
}
