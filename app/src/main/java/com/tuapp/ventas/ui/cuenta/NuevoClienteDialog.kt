package com.tuapp.ventas.ui.cuenta

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.R
import com.tuapp.ventas.data.model.Cliente
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.databinding.DialogNuevoClienteBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NuevoClienteDialog : DialogFragment() {
    var onCrear: ((nombre: String, telefono: String?, mesa: String?, recordarCuenta: Boolean) -> Unit)? = null
    private val repo: VentasRepository by lazy { (requireActivity().application as VentasApplication).repository }
    private var clientesCache: Map<String, Cliente> = emptyMap()
    private var busquedaJob: Job? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNuevoClienteBinding.inflate(layoutInflater)
        configurarAutocomplete(binding)
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

    private fun configurarAutocomplete(binding: DialogNuevoClienteBinding) {
        val adapter = ArrayAdapter<String>(
            requireContext(),
            R.layout.item_cliente_suggestion,
            mutableListOf()
        )
        binding.inputNombre.setAdapter(adapter)
        binding.inputNombre.threshold = 2

        binding.inputNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                busquedaJob?.cancel()
                if (query.length < 2) {
                    clientesCache = emptyMap()
                    adapter.clear()
                    return
                }

                // Pequeña espera para no consultar Room en cada tecla cuando el usuario escribe rápido.
                busquedaJob = lifecycleScope.launch {
                    delay(250)
                    val clientes = repo.buscarClientesPorNombre(query)
                    clientesCache = clientes.associateBy { it.nombre }
                    adapter.clear()
                    adapter.addAll(clientes.map { it.nombre })
                    adapter.notifyDataSetChanged()
                    if (clientes.isNotEmpty()) binding.inputNombre.showDropDown()
                }
            }
        })

        binding.inputNombre.setOnItemClickListener { _, _, position, _ ->
            val nombreSeleccionado = adapter.getItem(position).orEmpty()
            val cliente = clientesCache[nombreSeleccionado] ?: return@setOnItemClickListener

            // Al seleccionar un cliente recurrente se rellenan los datos conocidos.
            binding.inputNombre.setText(cliente.nombre, false)
            binding.inputTelefono.setText(cliente.telefono.orEmpty())
            binding.inputMesa.setText(cliente.mesa.orEmpty())
            binding.checkRecordarCuenta.isChecked = cliente.recordarCuenta
        }
    }
}
