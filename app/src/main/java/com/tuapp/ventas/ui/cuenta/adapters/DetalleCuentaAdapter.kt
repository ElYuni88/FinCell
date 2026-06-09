package com.tuapp.ventas.ui.cuenta.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.R
import com.tuapp.ventas.data.model.relaciones.DetalleConProducto
import com.tuapp.ventas.databinding.ItemDetalleCuentaBinding
import com.tuapp.ventas.utils.DateUtils

/** Adapter con edición de cantidades y eliminación para cuentas abiertas. */
class DetalleCuentaAdapter(
    private val soloLectura: Boolean = false,
    private val onIncrementar: (DetalleConProducto) -> Unit = {},
    private val onDecrementar: (DetalleConProducto) -> Unit = {},
    private val onEliminar: (DetalleConProducto) -> Unit = {}
) : ListAdapter<DetalleConProducto, DetalleCuentaAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<DetalleConProducto>() {
        override fun areItemsTheSame(oldItem: DetalleConProducto, newItem: DetalleConProducto) = oldItem.detalle.id == newItem.detalle.id
        override fun areContentsTheSame(oldItem: DetalleConProducto, newItem: DetalleConProducto) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemDetalleCuentaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemDetalleCuentaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DetalleConProducto) = with(binding) {
            txtProducto.text = item.producto.nombre
            txtCantidad.text = item.detalle.cantidad.toString()
            txtSubtotal.text = "Subtotal: ${DateUtils.moneda(item.detalle.subtotal)}"
            btnMenos.visibility = if (soloLectura) View.GONE else View.VISIBLE
            btnMas.visibility = if (soloLectura) View.GONE else View.VISIBLE
            btnEliminar.visibility = if (soloLectura) View.GONE else View.VISIBLE
            btnMenos.isEnabled = item.detalle.cantidad > 1
            btnMas.isEnabled = item.detalle.cantidad < 99
            btnMenos.setOnClickListener { animar(it); onDecrementar(item) }
            btnMas.setOnClickListener { animar(it); onIncrementar(item) }
            btnEliminar.setOnClickListener { animar(it); onEliminar(item) }
        }

        private fun animar(view: View) {
            view.startAnimation(AnimationUtils.loadAnimation(view.context, R.anim.anim_bounce))
        }
    }
}
