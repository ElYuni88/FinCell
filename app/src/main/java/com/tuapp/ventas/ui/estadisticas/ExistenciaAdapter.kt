package com.tuapp.ventas.ui.estadisticas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.R
import com.tuapp.ventas.data.model.ExistenciaProducto
import com.tuapp.ventas.databinding.ItemExistenciaProductoBinding
import com.tuapp.ventas.utils.DateUtils

class ExistenciaAdapter : ListAdapter<ExistenciaProducto, ExistenciaAdapter.ExistenciaViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExistenciaViewHolder {
        val binding = ItemExistenciaProductoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExistenciaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExistenciaViewHolder, position: Int) =
        holder.bind(getItem(position))

    class ExistenciaViewHolder(private val binding: ItemExistenciaProductoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExistenciaProducto) = with(binding) {
            val ctx = root.context

            // Nombre + sufijo de estado
            val sufijo = when {
                item.esEliminado -> " (eliminado)"
                item.esInactivo -> " (inactivo)"
                else -> ""
            }
            txtNombreProducto.text = item.nombre + sufijo

            // Stock inicial
            txtStockInicial.text = item.stockHistorico?.toString() ?: "—"
            txtStockInicial.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (item.stockHistorico == null) R.color.text_secondary else R.color.text_primary
                )
            )

            // Stock actual: color según comparación con histórico
            val actual = item.stockActual
            if (actual == null) {
                txtStockActual.text = "—"
                txtStockActual.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            } else {
                txtStockActual.text = actual.toString()
                val color = when {
                    item.stockHistorico == null -> R.color.text_primary
                    actual > item.stockHistorico -> R.color.success
                    actual < item.stockHistorico -> R.color.colorError
                    else -> R.color.text_secondary
                }
                txtStockActual.setTextColor(ContextCompat.getColor(ctx, color))
            }

            // Precio
            txtPrecio.text = DateUtils.moneda(item.precio)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ExistenciaProducto>() {
        override fun areItemsTheSame(oldItem: ExistenciaProducto, newItem: ExistenciaProducto): Boolean =
            oldItem.codigoBarras == newItem.codigoBarras

        override fun areContentsTheSame(oldItem: ExistenciaProducto, newItem: ExistenciaProducto): Boolean =
            oldItem == newItem
    }
}