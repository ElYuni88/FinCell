package com.tuapp.ventas.ui.ipb

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.R
import com.tuapp.ventas.data.model.ProductoAnalisis
import com.tuapp.ventas.databinding.ItemProductoAnalisisBinding
import com.tuapp.ventas.utils.DateUtils

class ProductoAnalisisAdapter(
    private val onCeldaClick: (fila: Int, columna: Int) -> Unit
) : ListAdapter<ProductoAnalisis, ProductoAnalisisAdapter.VH>(Diff) {

    var filaSeleccionada: Int? = null
        private set

    var columnaSeleccionada: Int? = null
        private set

    fun toggleSeleccion(fila: Int, columna: Int) {
        if (filaSeleccionada == fila && columnaSeleccionada == columna) {
            filaSeleccionada = null
            columnaSeleccionada = null
        } else {
            filaSeleccionada = fila
            columnaSeleccionada = columna
        }
        notifyDataSetChanged()
    }

    fun limpiarSeleccion() {
        if (filaSeleccionada != null || columnaSeleccionada != null) {
            filaSeleccionada = null
            columnaSeleccionada = null
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemProductoAnalisisBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class VH(private val binding: ItemProductoAnalisisBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductoAnalisis, position: Int) = with(binding) {
            val ctx = root.context

            val esFilaSel = filaSeleccionada == position
            val esColSel = columnaSeleccionada

            // ── Contenido ────────────────────────────────────────
            val sufijo = if (item.inactivo) " (inactivo)" else ""
            txtProducto.text = item.nombre + sufijo
            txtPrecio.text = DateUtils.moneda(item.precio) + "/u."

            if (item.entrada > 0) {
                celdaEntrada.text = "+${item.entrada}"
                celdaEntrada.setTextColor(ContextCompat.getColor(ctx, R.color.success))
            } else {
                celdaEntrada.text = "—"
                celdaEntrada.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            }

            celdaStockInicial.text = item.stockInicial.toString()

            txtVendidos.text = item.vendidosTotal.toString()
            if (item.vendidosMuerto > 0) {
                txtVendidosMuerto.visibility = View.VISIBLE
                txtVendidosMuerto.text = "(+${item.vendidosMuerto} del muerto)"
            } else {
                txtVendidosMuerto.visibility = View.GONE
            }

            celdaStockFinal.text = item.stockFinal.toString()
            celdaSubtotal.text = DateUtils.moneda(item.subtotal)

            // ── Background según selección ───────────────────────
            val colorSoft = ContextCompat.getColor(ctx, R.color.analisis_highlight_soft)
            val colorStrong = ContextCompat.getColor(ctx, R.color.analisis_highlight_strong)
            val colorTransparente = Color.TRANSPARENT

            fun bgCelda(col: Int): Int {
                val enFila = esFilaSel
                val enCol = esColSel == col
                return when {
                    enFila && enCol -> colorStrong     // celda exacta
                    enFila || enCol -> colorSoft       // fila o columna
                    else -> colorTransparente
                }
            }

            celdaProducto.setBackgroundColor(bgCelda(0))
            celdaEntrada.setBackgroundColor(bgCelda(1))
            celdaStockInicial.setBackgroundColor(bgCelda(2))
            celdaVendidos.setBackgroundColor(bgCelda(3))
            celdaStockFinal.setBackgroundColor(bgCelda(4))
            celdaSubtotal.setBackgroundColor(bgCelda(5))

            // ── Texto blanco en la celda exacta ──────────────────
            val colorTextoNormal = ContextCompat.getColor(ctx, R.color.text_primary)
            val colorTextoSecundario = ContextCompat.getColor(ctx, R.color.text_secondary)
            val esCeldaExacta = { col: Int -> esFilaSel && esColSel == col }

            txtProducto.setTextColor(if (esCeldaExacta(0)) Color.WHITE else colorTextoNormal)
            txtPrecio.setTextColor(if (esCeldaExacta(0)) Color.WHITE else colorTextoSecundario)

            celdaEntrada.setTextColor(
                if (esCeldaExacta(1)) Color.WHITE
                else if (item.entrada > 0) ContextCompat.getColor(ctx, R.color.success)
                else colorTextoSecundario
            )

            celdaStockInicial.setTextColor(if (esCeldaExacta(2)) Color.WHITE else colorTextoNormal)

            txtVendidos.setTextColor(if (esCeldaExacta(3)) Color.WHITE else colorTextoNormal)
            txtVendidosMuerto.setTextColor(if (esCeldaExacta(3)) Color.WHITE else colorTextoSecundario)

            celdaStockFinal.setTextColor(if (esCeldaExacta(4)) Color.WHITE else colorTextoNormal)
            celdaSubtotal.setTextColor(if (esCeldaExacta(5)) Color.WHITE else colorTextoNormal)

            // ── Listeners ────────────────────────────────────────
            celdaProducto.setOnClickListener { onCeldaClick(position, 0) }
            celdaEntrada.setOnClickListener { onCeldaClick(position, 1) }
            celdaStockInicial.setOnClickListener { onCeldaClick(position, 2) }
            celdaVendidos.setOnClickListener { onCeldaClick(position, 3) }
            celdaStockFinal.setOnClickListener { onCeldaClick(position, 4) }
            celdaSubtotal.setOnClickListener { onCeldaClick(position, 5) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ProductoAnalisis>() {
        override fun areItemsTheSame(oldItem: ProductoAnalisis, newItem: ProductoAnalisis): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ProductoAnalisis, newItem: ProductoAnalisis): Boolean =
            oldItem == newItem
    }
}