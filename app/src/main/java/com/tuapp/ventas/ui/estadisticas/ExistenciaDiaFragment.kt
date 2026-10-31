package com.tuapp.ventas.ui.estadisticas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.FragmentExistenciaDiaBinding
import com.tuapp.ventas.utils.DateUtils

class ExistenciaDiaFragment : Fragment() {
    private var _binding: FragmentExistenciaDiaBinding? = null
    private val binding get() = _binding!!

    private val adapter = ExistenciaAdapter()

    private val viewModel: EstadisticasViewModel by activityViewModels {
        EstadisticasViewModelFactory(
            (requireActivity().application as VentasApplication).repository,
            requireContext().applicationContext
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExistenciaDiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerExistencia.setHasFixedSize(true)
        binding.recyclerExistencia.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExistencia.adapter = adapter

        viewModel.existenciaPeriodo.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)

            val vacia = lista.isEmpty()
            binding.txtVacio.visibility = if (vacia) View.VISIBLE else View.GONE
            binding.recyclerExistencia.visibility = if (vacia) View.GONE else View.VISIBLE

            // Badge: fecha de inicio del período
            val conHistorico = lista.firstOrNull { it.esHistorico }
            if (conHistorico != null) {
                binding.layoutBadgeSnapshot.visibility = View.VISIBLE
                binding.txtBadgeSnapshot.text = "📸 Inicio del período: ${DateUtils.fechaHora(viewModel.obtenerInicioPeriodoActual())}"
                binding.txtHeaderInicial.text = "📸 Stock Inicial"
            } else {
                binding.layoutBadgeSnapshot.visibility = View.GONE
                binding.txtHeaderInicial.text = "Stock Inicial"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.recargar()
    }

    override fun onDestroyView() {
        binding.recyclerExistencia.adapter = null
        _binding = null
        super.onDestroyView()
    }
}