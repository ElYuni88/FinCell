package com.tuapp.ventas.ui.cuenta

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tuapp.ventas.databinding.DialogNuevoClienteBinding

class NuevoClienteDialog : DialogFragment() {
    var onCrear: ((nombre: String, telefono: String?, mesa: String?, recordarCuenta: Boolean) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNuevoClienteBinding.inflate(layoutInflater)
        return AlertDialog.Builder(requireContext())
            .setTitle("Nueva cuenta")
            .setView(binding.root)
            .setPositiveButton("Crear", null)
            .setNegativeButton("Cancelar", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nombre = binding.inputNombre.text?.toString()?.trim().orEmpty()
                        val telefono = binding.inputTelefono.text?.toString()?.trim()?.ifBlank { null }
                        val mesa = binding.inputMesa.text?.toString()?.trim()?.ifBlank { null }
                        val recordar = binding.checkRecordarCuenta.isChecked

                        if (nombre.isBlank()) {
                            Toast.makeText(requireContext(), "El nombre es requerido", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        onCrear?.invoke(nombre, telefono, mesa, recordar)
                        dismiss()
                    }
                }
            }
    }
}
