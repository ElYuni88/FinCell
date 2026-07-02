package com.tuapp.ventas.ui.simple

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.DialogAgregarManualBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AgregarProductoManualDialog : DialogFragment() {
    var onConfirmar: ((Producto, Int) -> Unit)? = null
    private var productos: List<Producto> = emptyList()
    private var productoSeleccionado: Producto? = null
    private var _binding: DialogAgregarManualBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAgregarManualBinding.inflate(layoutInflater)
        cargarAutocompletado()
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("AGREGAR", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        confirmar(dialog)
                    }
                }
            }
    }

    private fun cargarAutocompletado() {
        lifecycleScope.launch {
            productos = (requireActivity().application as VentasApplication).repository.productosSinCodigo().first()
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, productos.map { it.nombre })
            binding.inputNombre.setAdapter(adapter)
            binding.inputNombre.threshold = 2
            binding.inputNombre.setOnItemClickListener { _, _, position, _ ->
                productos.firstOrNull { it.nombre == adapter.getItem(position) }?.let { p ->
                    productoSeleccionado = p
                    binding.inputNombre.setText(p.nombre, false)
                    binding.inputCodigoManual.setText(p.codigoBarras.orEmpty())
                    binding.inputPrecio.setText(p.precio.toString())
                    binding.txtInventario.text = "Inventario disponible: ${p.inventario}"
                    binding.txtInventario.visibility = View.VISIBLE
                    binding.txtSinResultados.visibility = View.GONE
                    binding.inputCantidad.setText(if (p.inventario > 0) "1" else "0")
                }
            }
            binding.inputNombre.setOnDismissListener {
                val nombre = binding.inputNombre.text?.toString()?.trim().orEmpty()
                if (nombre.isNotBlank() && productos.none { it.nombre.equals(nombre, true) }) {
                    binding.txtSinResultados.visibility = View.VISIBLE
                    productoSeleccionado = null
                }
            }
        }
    }

    private fun confirmar(dialog: AlertDialog) {
        val nombre = binding.inputNombre.text?.toString()?.trim().orEmpty()
        val producto = productoSeleccionado ?: productos.firstOrNull { it.nombre.equals(nombre, true) }
        if (producto == null) {
            binding.txtSinResultados.visibility = View.VISIBLE
            binding.inputNombre.error = "Seleccione un producto existente"
            return
        }
        val cantidad = binding.inputCantidad.text?.toString()?.toIntOrNull() ?: 1
        if (producto.inventario <= 0 || cantidad !in 1..producto.inventario) {
            binding.inputCantidad.error = "Máximo disponible: ${producto.inventario}"
            return
        }
        onConfirmar?.invoke(producto, cantidad)
        dialog.dismiss()
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
