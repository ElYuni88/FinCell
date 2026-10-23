package com.tuapp.ventas.ui.ticket

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.R
import com.tuapp.ventas.databinding.DialogTicketPreviewBinding
import com.tuapp.ventas.utils.DateUtils

class TicketPreviewDialog : DialogFragment() {

    var data: TicketData? = null
    var onExportarCompartir: ((TicketData) -> Unit)? = null
    var onImprimir: ((TicketData) -> Unit)? = null

    private var _binding: DialogTicketPreviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTicketPreviewBinding.inflate(LayoutInflater.from(requireContext()))
        val d = data ?: error("TicketData no asignado")

        with(binding) {
            txtBienvenida.text = "¡Bienvenidos a ${d.nombrePuntoVenta}!"
            txtCuenta.text = "Cuenta: #${d.cuentaId}"
            txtCliente.text = "Cliente: ${d.nombreCliente}"
            txtMesa.text = "Mesa: ${d.mesa}"
            txtApertura.text = "Apertura: ${DateUtils.fechaHora(d.fechaApertura)}"
            txtCierre.text = d.fechaCierre?.let { "Cierre: ${DateUtils.fechaHora(it)}" } ?: ""
            txtPago.text = "Pago: ${d.metodoPago}"
            txtObservaciones.text = d.observaciones?.takeIf { it.isNotBlank() }?.let { "Obs: $it" } ?: ""
            txtObservaciones.visibility = if (d.observaciones.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            txtTotal.text = DateUtils.moneda(d.total)

            contenedorProductos.removeAllViews()
            d.productos.forEach { p ->
                val row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_ticket_producto, contenedorProductos, false)

                row.findViewById<TextView>(R.id.txtProductoNombre).text = p.nombre

                val detalle = if (p.cantidad > 1) {
                    "${p.cantidad} × ${DateUtils.moneda(p.precioUnitario)} = ${DateUtils.moneda(p.subtotal)}"
                } else {
                    DateUtils.moneda(p.subtotal)
                }
                row.findViewById<TextView>(R.id.txtProductoDetalle).text = detalle

                contenedorProductos.addView(row)
            }

            btnExportarCompartir.setOnClickListener { onExportarCompartir?.invoke(d) }
            btnImprimir.setOnClickListener { onImprimir?.invoke(d) }
            btnCancelar.setOnClickListener { dismiss() }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
            .apply {
                setOnShowListener {
                    val metrics = resources.displayMetrics
                    window?.setLayout(
                        metrics.widthPixels - 48,
                        (metrics.heightPixels * 0.85).toInt()
                    )
                }
            }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}