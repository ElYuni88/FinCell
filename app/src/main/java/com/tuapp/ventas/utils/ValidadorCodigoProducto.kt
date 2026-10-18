package com.tuapp.ventas.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import com.tuapp.ventas.R
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.databinding.DialogAgregarProductoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configura la validación reactiva del código de barras en el diálogo de alta de producto.
 *
 * Reglas:
 * - Si el campo está vacío → estado Vacio (no se puede guardar)
 * - Si el código tiene caracteres inválidos → estado Invalido (no se puede guardar)
 * - Si el código existe en la BD → estado Duplicado (no se puede guardar, aviso rojo)
 * - Si el código es solo letras → estado SoloLetras (aviso amarillo, SÍ se puede guardar)
 * - Si todo está OK → estado Valido (se puede guardar)
 */
object ValidadorCodigoProducto {

    private const val DEBOUNCE_MS = 250L

    sealed class Estado {
        object Vacio : Estado()
        object Invalido : Estado()
        data class Duplicado(val nombreExistente: String) : Estado()
        data class SoloLetras(val codigoNormalizado: String) : Estado()
        data class Valido(val codigoNormalizado: String) : Estado()
    }

    /**
     * Configura la validación. Devuelve el Job activo (por si se necesita cancelar).
     *
     * @param binding Binding del diálogo de alta.
     * @param repo Repositorio para consultar duplicados.
     * @param scope Scope para las corrutinas (normalmente lifecycleScope).
     * @param productoOriginalId Si estamos EDITANDO, el ID del producto original (para excluirse a sí mismo de la validación de duplicado).
     * @param campoEditable Si el campo es editable (alta manual) o no (viene del escáner).
     * @param onEstadoCambiado Callback que se llama cada vez que cambia el estado. Recibe el estado actual.
     * @param onCodigoNormalizado Callback opcional que recibe el código normalizado para actualizar el campo visible.
     */
    fun configurar(
        binding: DialogAgregarProductoBinding,
        repo: VentasRepository,
        scope: CoroutineScope,
        productoOriginalId: Long? = null,
        campoEditable: Boolean,
        onEstadoCambiado: (Estado) -> Unit,
        onCodigoNormalizado: ((String) -> Unit)? = null
    ): Job? {
        var jobActual: Job? = null

        val validar = { textoRaw: String ->
            jobActual?.cancel()
            jobActual = scope.launch {
                val codigoNormalizado = CodigoBarrasUtils.normalizar(textoRaw)

                // Si el campo es editable, actualizamos visualmente al valor normalizado
                if (campoEditable && onCodigoNormalizado != null) {
                    if (codigoNormalizado != textoRaw) {
                        onCodigoNormalizado(codigoNormalizado)
                    }
                }

                // 1. Vacío
                if (codigoNormalizado.isBlank()) {
                    actualizarUI(binding, Estado.Vacio)
                    onEstadoCambiado(Estado.Vacio)
                    return@launch
                }

                // 2. Formato inválido (caracteres raros)
                if (!CodigoBarrasUtils.tieneFormatoValido(codigoNormalizado)) {
                    actualizarUI(binding, Estado.Invalido)
                    onEstadoCambiado(Estado.Invalido)
                    return@launch
                }

                // 3. Consultar duplicado (con debounce)
                delay(DEBOUNCE_MS)
                val existente = repo.buscarProductoPorCodigo(codigoNormalizado)
                if (existente != null && existente.id != productoOriginalId) {
                    actualizarUI(binding, Estado.Duplicado(existente.nombre))
                    onEstadoCambiado(Estado.Duplicado(existente.nombre))
                    return@launch
                }

                // 4. Solo letras (advertencia, no bloqueante)
                if (CodigoBarrasUtils.esSoloLetras(codigoNormalizado)) {
                    actualizarUI(binding, Estado.SoloLetras(codigoNormalizado))
                    onEstadoCambiado(Estado.SoloLetras(codigoNormalizado))
                    return@launch
                }

                // 5. Todo OK
                actualizarUI(binding, Estado.Valido(codigoNormalizado))
                onEstadoCambiado(Estado.Valido(codigoNormalizado))
            }
        }

        // Si el campo es editable, escuchamos los cambios
        if (campoEditable) {
            binding.inputCodigo.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun afterTextChanged(s: Editable?) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validar(s?.toString().orEmpty())
                }
            })
        }

        // Validación inicial (por si el campo ya trae un valor, como en el escáner)
        validar(binding.inputCodigo.text?.toString().orEmpty())

        return jobActual
    }

    /**
     * Actualiza la UI (aviso y borde del campo) según el estado.
     */
    private fun actualizarUI(binding: DialogAgregarProductoBinding, estado: Estado) {
        val ctx = binding.root.context

        when (estado) {
            is Estado.Vacio -> {
                binding.tvAviso.visibility = View.GONE
                binding.inputCodigo.setBackgroundResource(R.drawable.bg_stitch_card)
            }
            is Estado.Invalido -> {
                binding.tvAviso.visibility = View.VISIBLE
                binding.tvAviso.text = "⚠️ El código solo puede contener letras, números y guion medio (-)"
                binding.tvAviso.setBackgroundResource(R.drawable.bg_aviso_duplicado)
                binding.tvAviso.setTextColor(ContextCompat.getColor(ctx, R.color.colorError))
                binding.inputCodigo.setBackgroundResource(R.drawable.bg_aviso_duplicado)
            }
            is Estado.Duplicado -> {
                binding.tvAviso.visibility = View.VISIBLE
                binding.tvAviso.text = "⚠️ El código de barras ya está registrado como:\n\"${estado.nombreExistente}\""
                binding.tvAviso.setBackgroundResource(R.drawable.bg_aviso_duplicado)
                binding.tvAviso.setTextColor(ContextCompat.getColor(ctx, R.color.colorError))
                binding.inputCodigo.setBackgroundResource(R.drawable.bg_aviso_duplicado)
            }
            is Estado.SoloLetras -> {
                binding.tvAviso.visibility = View.VISIBLE
                binding.tvAviso.text = "💡 Se aconseja incluir números en el código de barras"
                binding.tvAviso.setBackgroundResource(R.drawable.bg_aviso_solo_letras)
                binding.tvAviso.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                binding.inputCodigo.setBackgroundResource(R.drawable.bg_stitch_card)
            }
            is Estado.Valido -> {
                binding.tvAviso.visibility = View.GONE
                binding.inputCodigo.setBackgroundResource(R.drawable.bg_stitch_card)
            }
        }
    }
}