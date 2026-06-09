package com.tuapp.ventas.ui.main.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.R
import com.tuapp.ventas.data.model.Cuenta
import com.tuapp.ventas.data.model.relaciones.CuentaResumen
import com.tuapp.ventas.databinding.ItemCuentaBinding
import com.tuapp.ventas.utils.DateUtils

/** Adapter Material 3 para cuentas abiertas/cerradas con colores de estado. */
class CuentasActivasAdapter(
    private val onClick: (CuentaResumen) -> Unit
) : ListAdapter<CuentaResumen, CuentasActivasAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<CuentaResumen>() {
        override fun areItemsTheSame(oldItem: CuentaResumen, newItem: CuentaResumen) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CuentaResumen, newItem: CuentaResumen) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemCuentaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemCuentaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cuenta: CuentaResumen) = with(binding) {
            val context = root.context
            val abierta = cuenta.estado == Cuenta.ESTADO_ABIERTA
            val fondo = if (abierta) R.color.card_background_abierta else R.color.card_background_cerrada
            val texto = if (abierta) R.color.cuenta_abierta_text else R.color.cuenta_cerrada_text

            cardCuenta.setCardBackgroundColor(ContextCompat.getColor(context, fondo))
            viewEstadoStripe.setBackgroundColor(ContextCompat.getColor(context, if (abierta) R.color.pos_red else R.color.pos_green))
            txtEstadoIcono.text = if (abierta) "🔴" else "🟢"
            txtNombreCliente.text = cuenta.nombreCliente
            txtNombreCliente.setTextColor(ContextCompat.getColor(context, texto))
            txtTotalCuenta.text = DateUtils.moneda(cuenta.total)
            txtTotalCuenta.setTextColor(ContextCompat.getColor(context, texto))
            txtMetaCuenta.text = "${cuenta.cantidadProductos} productos · ${DateUtils.fechaHora(cuenta.fechaApertura)}"
            btnAccionCuenta.text = if (abierta) "SELECCIONAR" else "VER DETALLE"
            btnAccionCuenta.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, if (abierta) R.color.error else R.color.success))
            btnAccionCuenta.setOnClickListener { onClick(cuenta) }
            root.setOnClickListener { onClick(cuenta) }
        }
    }
}
