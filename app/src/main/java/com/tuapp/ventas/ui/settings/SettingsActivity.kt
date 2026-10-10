package com.tuapp.ventas.ui.settings

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.R
import com.tuapp.ventas.VentasApplication
import com.tuapp.ventas.databinding.ActivitySettingsBinding
import com.tuapp.ventas.ui.base.BaseActivity
import com.tuapp.ventas.ui.main.MainActivity
import com.tuapp.ventas.utils.PreferencesManager
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        cargar()

        binding.btnGuardar.setOnClickListener {
            guardar()
            finish()
        }

        binding.btnVolver.setOnClickListener { finish() }

        binding.btnResetearApp.setOnClickListener {
            mostrarPrimeraConfirmacion()
        }
    }

    private fun cargar() {
        binding.switchSonido.isChecked = prefs.sonidoEscaneo
        binding.switchVibrar.isChecked = prefs.vibrarEscaneo
        binding.switchConfirmar.isChecked = prefs.confirmarCuenta
        binding.switchBackup.isChecked = prefs.backupAutomatico
        binding.radioRecordar.isChecked = prefs.modoPredeterminado == "RECORDAR"
        binding.radioSimple.isChecked = prefs.modoPredeterminado == "SIMPLE"
        binding.radioCuenta.isChecked = prefs.modoPredeterminado == "CUENTA"
    }

    private fun guardar() {
        prefs.sonidoEscaneo = binding.switchSonido.isChecked
        prefs.vibrarEscaneo = binding.switchVibrar.isChecked
        prefs.confirmarCuenta = binding.switchConfirmar.isChecked
        prefs.backupAutomatico = binding.switchBackup.isChecked
        prefs.modoPredeterminado = when {
            binding.radioSimple.isChecked -> "SIMPLE"
            binding.radioCuenta.isChecked -> "CUENTA"
            else -> "RECORDAR"
        }
    }

    // ============================================================
    // RESETEO DE LA APP (doble confirmación)
    // ============================================================

    private fun mostrarPrimeraConfirmacion() {
        val mensaje = """
            ⚠️ ESTA ACCIÓN ES IRREVERSIBLE ⚠️

            Se borrará PERMANENTEMENTE:
            • Todos los productos
            • Todas las ventas (directas y cuentas)
            • Todo el historial de ventas cerradas
            • Todas las notificaciones
            • El historial de IPV importados
            • Los ajustes de gastos/ingresos por día

            Se CONSERVARÁ:
            • La licencia activa
            • El Punto de Venta
            • Los clientes guardados
            • La configuración de la app

            ¿Deseas continuar?
        """.trimIndent()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Resetear aplicación")
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar") { _, _ ->
                mostrarSegundaConfirmacion()
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Color.parseColor("#F44336"))
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(Color.parseColor("#666666"))
        }
        dialog.show()
    }

    private fun mostrarSegundaConfirmacion() {
        // Input para escribir "RESET"
        val input = EditText(this).apply {
            hint = "Escribe RESET en mayúsculas"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setPadding(40, 30, 40, 30)
        }

        val container = FrameLayout(this).apply {
            setPadding(40, 20, 40, 0)
            addView(input)
        }

        val mensaje = """
            ⚠️ ÚLTIMA CONFIRMACIÓN ⚠️

            Esta acción NO se puede deshacer.
            Toda la información borrada será IRRECUPERABLE.

            Para confirmar, escribe la palabra RESET:

            (Los clientes, la licencia y el Punto de Venta se conservarán)
        """.trimIndent()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Confirmación final")
            .setMessage(mensaje)
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("RESETEAR") { _, _ ->
                val texto = input.text.toString().trim()
                if (texto != "RESET") {
                    Toast.makeText(
                        this,
                        "❌ Debes escribir RESET exactamente para confirmar",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }
                ejecutarReseteo()
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Color.parseColor("#F44336"))
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(Color.parseColor("#666666"))
            input.requestFocus()
        }
        dialog.show()
    }

    private fun ejecutarReseteo() {
        lifecycleScope.launch {
            try {
                val repo = (application as VentasApplication).repository
                val resultado = repo.resetearApp()

                if (resultado.exito) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "✅ ${resultado.mensaje}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Ir a la pantalla de inicio limpia
                    val intent = Intent(this@SettingsActivity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    finishAffinity()
                } else {
                    Toast.makeText(
                        this@SettingsActivity,
                        "❌ ${resultado.mensaje}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    "❌ Error al resetear: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}