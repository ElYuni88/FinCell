package com.tuapp.ventas.ui.cuenta

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tuapp.ventas.databinding.DialogNuevoClienteBinding

class NuevoClienteDialog : DialogFragment() {
    var onCrear: ((String, String?) -> Unit)? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNuevoClienteBinding.inflate(layoutInflater)
        return AlertDialog.Builder(requireContext()).setTitle("Nueva cuenta")
            .setView(binding.root).setPositiveButton("Crear") { _, _ -> onCrear?.invoke(binding.inputNombre.text.toString().ifBlank { "Cliente" }, binding.inputTelefono.text?.toString()) }
            .setNegativeButton("Cancelar", null).create()
    }
}
