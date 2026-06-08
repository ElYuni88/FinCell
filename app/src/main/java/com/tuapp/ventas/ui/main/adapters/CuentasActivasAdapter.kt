package com.tuapp.ventas.ui.main.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.Cuenta
import com.tuapp.ventas.databinding.ItemCuentaActivaBinding
import com.tuapp.ventas.utils.DateUtils

class CuentasActivasAdapter(private val onClick: (Cuenta) -> Unit) : ListAdapter<Cuenta, CuentasActivasAdapter.VH>(Diff) {
    object Diff : DiffUtil.ItemCallback<Cuenta>() { override fun areItemsTheSame(o: Cuenta, n: Cuenta) = o.id == n.id; override fun areContentsTheSame(o: Cuenta, n: Cuenta) = o == n }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemCuentaActivaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    inner class VH(private val binding: ItemCuentaActivaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cuenta: Cuenta) { binding.txtCuenta.text = "Cuenta #${cuenta.id}"; binding.txtTotal.text = DateUtils.moneda(cuenta.total); binding.root.setOnClickListener { onClick(cuenta) } }
    }
}
