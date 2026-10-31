package com.tuapp.ventas.ui.ipb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tuapp.ventas.R
import com.tuapp.ventas.data.model.GastoDesglose
import com.tuapp.ventas.databinding.DialogGastosDesgloseBinding
import com.tuapp.ventas.utils.DateUtils

class GastosDesgloseDialogFragment : BottomSheetDialogFragment() {

    var gastos: List<GastoDesglose> = emptyList()

    private var _binding: DialogGastosDesgloseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogGastosDesgloseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.contenedorGastos.removeAllViews()

        if (gastos.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "No hay gastos registrados en este período"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                setPadding(0, 24, 0, 24)
                textSize = 14f
            }
            binding.contenedorGastos.addView(tv)
        } else {
            gastos.forEach { gasto ->
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 10, 0, 10)
                }

                val nombre = TextView(requireContext()).apply {
                    text = gasto.nombre
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    textSize = 15f
                }

                val monto = TextView(requireContext()).apply {
                    text = DateUtils.moneda(gasto.monto)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.colorError))
                    textSize = 15f
                    typeface = android.graphics.Typeface.MONOSPACE
                }

                row.addView(nombre)
                row.addView(monto)
                binding.contenedorGastos.addView(row)
            }
        }

        binding.txtTotalGastos.text = DateUtils.moneda(gastos.sumOf { it.monto })
        binding.btnCerrarGastos.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(gastos: List<GastoDesglose>) = GastosDesgloseDialogFragment().apply {
            this.gastos = gastos
        }
    }
}