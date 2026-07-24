package com.tuapp.ventas.ui.estadisticas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.FragmentResumenDiaBinding
import com.tuapp.ventas.utils.DateUtils

/** Fragmento que observa la fecha seleccionada y muestra el resumen de ventas del día. */
class ResumenDiaFragment : Fragment() {
    private var _binding: FragmentResumenDiaBinding? = null
    private val binding get() = _binding!!
    private val adapter = ResumenAdapter()
    private val viewModel: EstadisticasViewModel by activityViewModels {
        EstadisticasViewModelFactory((requireActivity().application as VentasApplication).repository, requireContext().applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResumenDiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerResumen.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResumen.adapter = adapter
        viewModel.resumenDia.observe(viewLifecycleOwner) { resumen ->
            adapter.submitList(resumen)
            binding.txtTotalResumen.text = "TOTAL: ${DateUtils.moneda(resumen.sumOf { it.subtotal })}"
        }
    }

    override fun onDestroyView() {
        binding.recyclerResumen.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
