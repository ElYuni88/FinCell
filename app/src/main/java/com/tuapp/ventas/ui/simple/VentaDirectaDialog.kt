package com.tuapp.ventas.ui.simple

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.tuapp.ventas.R
import com.tuapp.ventas.data.model.ModoOperacion
import com.tuapp.ventas.data.model.Producto
import com.tuapp.ventas.databinding.DialogVentaDirectaBinding
import com.tuapp.ventas.utils.DateUtils

/**
 * Diálogo Material para registrar ventas o agregar productos a cuenta.
 * La cantidad siempre está habilitada (1-99) y el subtotal se recalcula en vivo.
 */
class VentaDirectaDialog : DialogFragment() {
    var producto: Producto? = null
    var codigoNuevo: String? = null
    var modo: ModoOperacion = ModoOperacion.SIMPLE
    var onConfirmar: ((Producto?, String?, String?, Double?, Int) -> Unit)? = null
    var onVerCuenta: (() -> Unit)? = null

    private var cantidad = 1
    private var _binding: DialogVentaDirectaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogVentaDirectaBinding.inflate(layoutInflater)
        cantidad = savedInstanceState?.getInt(KEY_CANTIDAD) ?: 1
        configurarVista()
        val builder = AlertDialog.Builder(requireContext()).setView(binding.root)
        if (modo == ModoOperacion.CUENTA) builder.setNeutralButton("Ver cuenta") { _, _ -> onVerCuenta?.invoke() }
        return builder
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton(if (modo == ModoOperacion.SIMPLE) "REGISTRAR VENTA" else "AGREGAR") { _, _ -> confirmar() }
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    val positivo = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negativo = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    positivo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    positivo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pos_green))
                    negativo.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    negativo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.pos_gray_button))
                }
            }
    }

    private fun configurarVista() = with(binding) {
        val esNuevo = producto == null
        txtTitulo.text = if (esNuevo) "🍺 Producto nuevo" else "🍺 ${producto?.nombre.orEmpty()}"
        txtCodigo.text = "Código: ${producto?.codigoBarras ?: codigoNuevo.orEmpty()}"
        inputNombre.setText(producto?.nombre.orEmpty())
        inputPrecio.setText(producto?.precio?.toString().orEmpty())
        inputNombre.isEnabled = esNuevo
        inputPrecio.isEnabled = esNuevo
        btnMenos.setOnClickListener { cambiarCantidad(-1) }
        btnMas.setOnClickListener { cambiarCantidad(1) }
        inputPrecio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = actualizarSubtotal()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        actualizarSubtotal()
    }

    private fun cambiarCantidad(delta: Int) {
        val nueva = (cantidad + delta).coerceIn(1, 99)
        if (nueva == cantidad) return
        cantidad = nueva
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_bounce)
        if (delta > 0) binding.btnMas.startAnimation(anim) else binding.btnMenos.startAnimation(anim)
        actualizarSubtotal()
    }

    private fun actualizarSubtotal() = with(binding) {
        txtCantidad.text = cantidad.toString()
        val precio = inputPrecio.text?.toString()?.toDoubleOrNull() ?: 0.0
        txtSubtotal.text = "Subtotal: ${DateUtils.moneda(precio * cantidad)}"
        btnMenos.isEnabled = cantidad > 1
        btnMas.isEnabled = cantidad < 99
    }

    private fun confirmar() {
        val precio = binding.inputPrecio.text?.toString()?.toDoubleOrNull()
        onConfirmar?.invoke(producto, codigoNuevo, binding.inputNombre.text?.toString(), precio, cantidad.coerceIn(1, 99))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_CANTIDAD, cantidad)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object { private const val KEY_CANTIDAD = "cantidad" }
}
