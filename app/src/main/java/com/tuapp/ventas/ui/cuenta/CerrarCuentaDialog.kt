package com.tuapp.ventas.ui.cuenta

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tuapp.ventas.databinding.DialogCerrarCuentaBinding

class CerrarCuentaDialog : DialogFragment() {
    var total: Double = 0.0
    var onCerrar: ((String, String?) -> Unit)? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCerrarCuentaBinding.inflate(layoutInflater)
        binding.txtTotal.text = "$%.2f".format(total)
        val metodos = listOf("EFECTIVO", "TARJETA", "TRANSFERENCIA")
        binding.spinnerMetodo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, metodos)
        return AlertDialog.Builder(requireContext()).setTitle("Cerrar cuenta")
            .setView(binding.root).setPositiveButton("Cerrar cuenta") { _, _ -> onCerrar?.invoke(binding.spinnerMetodo.selectedItem.toString(), binding.inputObservaciones.text?.toString()) }
            .setNegativeButton("Cancelar", null).create()
    }
}
