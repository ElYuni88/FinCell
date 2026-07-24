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

/** Fragmento que muestra la existencia actual de productos para la fecha seleccionada. */
class ExistenciaDiaFragment : Fragment() {
    private var _binding: FragmentExistenciaDiaBinding? = null
    private val binding get() = _binding!!
    private val adapter = ExistenciaAdapter()
    private val viewModel: EstadisticasViewModel by activityViewModels {
        EstadisticasViewModelFactory((requireActivity().application as VentasApplication).repository, requireContext().applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExistenciaDiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerExistencia.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExistencia.adapter = adapter
        viewModel.existenciaDia.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() {
        binding.recyclerExistencia.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
