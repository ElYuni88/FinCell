package com.tuapp.ventas.ui.ipb

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.Gasto
import com.tuapp.ventas.databinding.ItemGastoBinding
import com.tuapp.ventas.utils.DateUtils

/** Adaptador de solo lectura para los gastos del resumen IPB. */
class GastoAdapter(private var gastos: List<Gasto>) : RecyclerView.Adapter<GastoAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemGastoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGastoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gasto = gastos[position]
        holder.binding.tvCategoria.text = gasto.categoria
        holder.binding.tvMonto.text = DateUtils.moneda(gasto.monto)
    }

    override fun getItemCount(): Int = gastos.size

    fun submitList(nuevosGastos: List<Gasto>) {
        gastos = nuevosGastos
        notifyDataSetChanged()
    }
}
