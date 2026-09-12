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

    // ✅ NUEVO: Guardar el cliente seleccionado actualmente
    private var clienteSeleccionado: Cliente? = null

    // ✅ NUEVO: Flag para evitar que se dispare la búsqueda al autocompletar
    private var ignorarCambiosNombre = false

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

                        // ✅ CAMBIO: Si es un cliente existente, no volver a guardarlo
                        val esClienteExistente = clienteSeleccionado != null
                                && clienteSeleccionado!!.nombre.equals(nombre, ignoreCase = true)

                        // Si es existente, no forzar recordar (ya está guardado)
                        val recordarFinal = if (esClienteExistente) false else recordar

                        onCrear?.invoke(nombre, telefono, mesa, recordarFinal)
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
                if (ignorarCambiosNombre) return

                val query = s?.toString()?.trim().orEmpty()

                if (clienteSeleccionado != null
                    && !clienteSeleccionado!!.nombre.equals(query, ignoreCase = true)) {
                    clienteSeleccionado = null
                }

                busquedaJob?.cancel()
                if (query.length < 2) {
                    clientesCache = emptyMap()
                    adapter.clear()
                    adapter.notifyDataSetChanged()
                    return
                }

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

        binding.inputNombre.setOnItemClickListener { parent, _, position, _ ->
            val nombreSeleccionado = adapter.getItem(position).orEmpty()
            val cliente = clientesCache[nombreSeleccionado] ?: return@setOnItemClickListener

            // 1. Ignorar cambios de texto
            ignorarCambiosNombre = true
            clienteSeleccionado = cliente

            // 2. Rellenar campos
            binding.inputNombre.setText(cliente.nombre, false)
            binding.inputTelefono.setText(cliente.telefono.orEmpty())
            binding.inputMesa.setText(cliente.mesa.orEmpty())
            binding.checkRecordarCuenta.isChecked = cliente.recordarCuenta

            // 3. Cerrar dropdown de múltiples formas (defensivo)
            binding.inputNombre.dismissDropDown()

            // 4. Forzar el cierre con post (a veces el filter lo reabre)
            binding.inputNombre.post {
                binding.inputNombre.dismissDropDown()
                binding.inputNombre.clearFocus()

                // Mover el foco a otro campo para evitar que se reabra
                binding.inputTelefono.requestFocus()
            }

            // 5. Cancelar cualquier búsqueda pendiente
            busquedaJob?.cancel()

            // 6. Restaurar el flag después de un tiempo
            binding.inputNombre.postDelayed({
                ignorarCambiosNombre = false
            }, 200)
        }

        // Cerrar dropdown si pierde foco
        binding.inputNombre.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.inputNombre.dismissDropDown()
            }
        }
    }
}