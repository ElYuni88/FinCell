package com.tuapp.ventas.ui.simple

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.DialogVentaDirectaBinding

class VentaDirectaDialog : DialogFragment() {
    var producto: Producto? = null
    var codigoNuevo: String? = null
    var modo: ModoOperacion = ModoOperacion.SIMPLE
    var onConfirmar: ((Producto?, String?, String?, Double?, Int) -> Unit)? = null
    var onVerCuenta: (() -> Unit)? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogVentaDirectaBinding.inflate(layoutInflater)
        val esNuevo = producto == null
        binding.inputNombre.setText(producto?.nombre.orEmpty())
        binding.inputPrecio.setText(producto?.precio?.toString().orEmpty())
        binding.inputNombre.isEnabled = esNuevo
        binding.inputPrecio.isEnabled = esNuevo
        binding.txtCodigo.text = producto?.codigoBarras ?: codigoNuevo.orEmpty()
        binding.txtTitulo.text = if (modo == ModoOperacion.SIMPLE) "Registrar venta directa" else "Agregar a cuenta"
        binding.selectorCantidad.minValue = 1; binding.selectorCantidad.maxValue = 99; binding.selectorCantidad.value = 1
        binding.selectorCantidad.isEnabled = modo == ModoOperacion.CUENTA
        val builder = AlertDialog.Builder(requireContext())
            .setView(binding.root)
        if (modo == ModoOperacion.CUENTA) builder.setNeutralButton("Ver cuenta") { _, _ -> onVerCuenta?.invoke() }
        return builder
            .setPositiveButton(if (modo == ModoOperacion.SIMPLE) "Registrar venta" else "Agregar") { _, _ ->
                onConfirmar?.invoke(producto, codigoNuevo, binding.inputNombre.text?.toString(), binding.inputPrecio.text?.toString()?.toDoubleOrNull(), binding.selectorCantidad.value)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
}
