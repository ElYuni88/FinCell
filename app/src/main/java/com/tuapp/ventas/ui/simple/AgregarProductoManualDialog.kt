package com.tuapp.ventas.ui.simple

import android.app.Dialog
import android.os.Bundle
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
            binding.inputNombre.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, productos.map { it.nombre }))
            binding.inputNombre.setOnItemClickListener { _, _, position, _ ->
                productos.getOrNull(position)?.let { p ->
                    binding.inputNombre.setText(p.nombre, false)
                    binding.inputCodigoManual.setText(p.codigoBarras.orEmpty())
                    binding.inputPrecio.setText(p.precio.toString())
                }
            }
        }
    }

    private fun confirmar(dialog: AlertDialog) {
        val nombre = binding.inputNombre.text?.toString()?.trim().orEmpty()
        val precio = binding.inputPrecio.text?.toString()?.toDoubleOrNull()
        val cantidad = binding.inputCantidad.text?.toString()?.toIntOrNull()?.coerceIn(1, 99) ?: 1
        if (nombre.isBlank() || precio == null) {
            binding.inputNombre.error = if (nombre.isBlank()) "Requerido" else null
            binding.inputPrecio.error = if (precio == null) "Precio inválido" else null
            return
        }
        val existente = productos.firstOrNull { it.nombre.equals(nombre, true) }
        val producto = existente ?: Producto(codigoBarras = binding.inputCodigoManual.text?.toString()?.trim().orEmpty(), nombre = nombre, precio = precio, tipoProducto = Producto.TIPO_MANUAL)
        onConfirmar?.invoke(producto, cantidad)
        dialog.dismiss()
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
