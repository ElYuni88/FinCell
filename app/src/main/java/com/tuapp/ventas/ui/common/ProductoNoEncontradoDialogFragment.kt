package com.tuapp.ventas.ui.common

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.DialogProductoNoEncontradoBinding

class ProductoNoEncontradoDialogFragment : DialogFragment() {
    var onDarEntrada: ((String) -> Unit)? = null
    var onProductoCreado: ((Producto) -> Unit)? = null
    private val codigo: String by lazy { requireArguments().getString(ARG_CODIGO).orEmpty() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogProductoNoEncontradoBinding.inflate(LayoutInflater.from(requireContext()))
        binding.txtCodigoBarras.text = codigo
        binding.btnDarEntrada.setOnClickListener {
            dismiss()
            onDarEntrada?.invoke(codigo)
        }
        binding.btnCancelar.setOnClickListener { dismiss() }
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    companion object {
        private const val ARG_CODIGO = "codigo"
        fun newInstance(codigo: String) = ProductoNoEncontradoDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_CODIGO, codigo) }
        }
    }
}
