package com.tuapp.ventas.ui.ipb

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.AjusteDia
import com.tuapp.ventas.data.model.CategoriaAjuste
import com.tuapp.ventas.databinding.ItemGastoAjusteBinding

class AjusteAdapter(
    private val onMontoCambiado: (AjusteDia) -> Unit,
    private val onActivoCambiado: (AjusteDia, Boolean) -> Unit,
    private val onEliminar: (AjusteDia) -> Unit
) : RecyclerView.Adapter<AjusteAdapter.ViewHolder>() {

    private val ajustes = mutableListOf<AjusteDia>()

    inner class ViewHolder(val binding: ItemGastoAjusteBinding) : RecyclerView.ViewHolder(binding.root) {
        private var watcher: TextWatcher? = null

        fun bind(ajuste: AjusteDia) {
            // Remover watcher anterior
            watcher?.let { binding.etMonto.removeTextChangedListener(it) }

            // Configurar nombre y checkbox
            binding.tvCategoria.text = ajuste.nombre

            // ✅ Mostrar/ocultar checkbox según si es persistente
            binding.chkActivo.visibility = View.VISIBLE
            binding.chkActivo.setOnCheckedChangeListener(null)
            binding.chkActivo.isChecked = ajuste.activo
            binding.chkActivo.setOnCheckedChangeListener { _, checked ->
                val posicion = adapterPosition
                if (posicion != RecyclerView.NO_POSITION) {
                    ajustes[posicion] = ajustes[posicion].copy(activo = checked)
                    onActivoCambiado(ajustes[posicion], checked)
                }
            }

            // Configurar monto
            binding.etMonto.setText(if (ajuste.monto == 0.0) "" else ajuste.monto.toString())

            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val posicion = adapterPosition
                    if (posicion != RecyclerView.NO_POSITION) {
                        val monto = s?.toString()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                        ajustes[posicion] = ajustes[posicion].copy(monto = monto)
                        onMontoCambiado(ajustes[posicion])
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            }
            binding.etMonto.addTextChangedListener(watcher)

            // Botón eliminar
            binding.btnEliminar.setOnClickListener {
                val posicion = adapterPosition
                if (posicion != RecyclerView.NO_POSITION) {
                    onEliminar(ajustes[posicion])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGastoAjusteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ajustes[position])
    }

    override fun getItemCount(): Int = ajustes.size

    fun submitList(nuevosAjustes: List<AjusteDia>) {
        ajustes.clear()
        ajustes.addAll(nuevosAjustes)
        notifyDataSetChanged()
    }

    fun obtenerAjustes(): List<AjusteDia> = ajustes.toList()
}