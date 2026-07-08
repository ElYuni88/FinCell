package com.tuapp.ventas.ui.simple

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.DialogAgregarManualBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AgregarProductoManualDialog : DialogFragment() {
    var onConfirmar: ((Producto, Int) -> Unit)? = null
    private var productos: List<Producto> = emptyList()
    private var productoSeleccionado: Producto? = null
    private var busquedaJob: Job? = null
    private var ignorarCambiosNombre = false
    private var _binding: DialogAgregarManualBinding? = null
    private val binding get() = _binding!!
    private val repo by lazy { (requireActivity().application as VentasApplication).repository }

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
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { confirmar(dialog) }
                }
            }
    }

    private fun cargarAutocompletado() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        binding.inputNombre.setAdapter(adapter)
        binding.inputNombre.threshold = 2
        binding.inputCodigoManual.isEnabled = false
        binding.inputPrecio.isEnabled = false

        binding.inputNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (ignorarCambiosNombre) return
                val query = s?.toString()?.trim().orEmpty()
                productoSeleccionado = null
                limpiarDatosProducto()
                (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                busquedaJob?.cancel()
                if (query.length < 2) {
                    productos = emptyList()
                    adapter.clear()
                    return
                }
                busquedaJob = lifecycleScope.launch {
                    delay(200)
                    productos = repo.buscarProductosManualesPorNombre(query)
                    adapter.clear()
                    if (productos.isEmpty()) {
                        adapter.add("Sin resultados")
                        binding.txtSinResultados.visibility = View.VISIBLE
                    } else {
                        adapter.addAll(productos.map { it.nombre })
                        binding.txtSinResultados.visibility = View.GONE
                    }
                    adapter.notifyDataSetChanged()
                    binding.inputNombre.showDropDown()
                }
            }
        })

        binding.inputNombre.setOnItemClickListener { _, _, position, _ ->
            val nombreSeleccionado = adapter.getItem(position).orEmpty()
            if (nombreSeleccionado == "Sin resultados") {
                productoSeleccionado = null
                ignorarCambiosNombre = true
                binding.inputNombre.setText("", false)
                ignorarCambiosNombre = false
                binding.txtSinResultados.visibility = View.VISIBLE
                return@setOnItemClickListener
            }
            productos.firstOrNull { it.nombre == nombreSeleccionado }?.let { p ->
                productoSeleccionado = p
                ignorarCambiosNombre = true
                binding.inputNombre.setText(p.nombre, false)
                ignorarCambiosNombre = false
                binding.inputNombre.dismissDropDown()
                binding.inputCodigoManual.setText(p.codigoBarras)
                binding.inputPrecio.setText(p.precio.toString())
                binding.txtInventario.text = "Stock disponible: ${p.inventario}"
                binding.txtInventario.visibility = View.VISIBLE
                binding.txtSinResultados.visibility = View.GONE
                binding.inputCantidad.setText(if (p.inventario > 0) "1" else "0")
                (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = p.inventario > 0
            }
        }
    }

    private fun limpiarDatosProducto() {
        binding.inputCodigoManual.setText("")
        binding.inputPrecio.setText("")
        binding.txtInventario.visibility = View.GONE
    }

    private fun confirmar(dialog: AlertDialog) {
        val producto = productoSeleccionado
        if (producto == null) {
            binding.txtSinResultados.visibility = View.VISIBLE
            binding.inputNombre.error = "Seleccione un producto existente"
            return
        }
        val cantidad = binding.inputCantidad.text?.toString()?.toIntOrNull() ?: 1
        if (producto.inventario <= 0 || cantidad !in 1..producto.inventario) {
            Toast.makeText(requireContext(), "La cantidad supera el inventario disponible (Stock: ${producto.inventario})", Toast.LENGTH_SHORT).show()
            binding.inputCantidad.error = "Máximo disponible: ${producto.inventario}"
            return
        }
        onConfirmar?.invoke(producto, cantidad)
        dialog.dismiss()
    }

    override fun onDestroyView() {
        busquedaJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
