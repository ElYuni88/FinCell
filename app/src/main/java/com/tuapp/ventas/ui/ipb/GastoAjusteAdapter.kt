package com.tuapp.ventas.ui.ipb

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.Gasto
import com.tuapp.ventas.databinding.ItemGastoAjusteBinding

/** Adaptador editable para administrar los gastos dinámicos del IPB. */
class GastoAjusteAdapter(private val gastos: MutableList<Gasto>) :
    RecyclerView.Adapter<GastoAjusteAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemGastoAjusteBinding) : RecyclerView.ViewHolder(binding.root) {
        private var watcher: TextWatcher? = null

        fun bind(gasto: Gasto) {
            watcher?.let { binding.etMonto.removeTextChangedListener(it) }
            binding.tvCategoria.text = gasto.categoria
            binding.etMonto.setText(if (gasto.monto == 0.0) "" else gasto.monto.toString())
            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val posicion = bindingAdapterPosition
                    if (posicion != RecyclerView.NO_POSITION) {
                        val monto = s?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                        gastos[posicion] = gastos[posicion].copy(monto = monto)
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            }
            binding.etMonto.addTextChangedListener(watcher)
            binding.btnEliminar.setOnClickListener {
                val posicion = bindingAdapterPosition
                if (posicion != RecyclerView.NO_POSITION) {
                    gastos.removeAt(posicion)
                    notifyItemRemoved(posicion)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGastoAjusteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(gastos[position])
    override fun getItemCount(): Int = gastos.size

    fun agregar(gasto: Gasto) {
        gastos.add(gasto)
        notifyItemInserted(gastos.lastIndex)
    }

    fun obtenerGastos(): List<Gasto> = gastos.toList()
}
