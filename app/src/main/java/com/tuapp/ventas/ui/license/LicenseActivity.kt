package com.tuapp.ventas.ui.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.databinding.ActivityLicenseBinding
import com.tuapp.ventas.ui.main.MainActivity
import com.tuapp.ventas.utils.LicenseManager
import com.tuapp.ventas.utils.PreferencesManager
import java.util.*

class LicenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLicenseBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)

        setContentView(binding.root)

        prefs = PreferencesManager(this)

        // 1. Mostrar ID del dispositivo
        val deviceId = LicenseManager.getDeviceId(this)
        binding.tvDeviceId.text = "ID de dispositivo: $deviceId"

        // Cargar código de punto de venta guardado (si existe)
        val savedPosId = LicenseManager.loadPosId(this) ?: ""
        binding.etCodigoPuntoVenta.setText(savedPosId)

        // 3. Botón "Solicitar licencia"
        binding.btnSolicitarLicencia.setOnClickListener {
            val deviceId2 = LicenseManager.getDeviceId(this)
            val codigoPV = binding.etCodigoPuntoVenta.text.toString().trim()
            if (codigoPV.isEmpty()) {
                Toast.makeText(this, "Ingresa el código del punto de venta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.codigoPuntoVenta = codigoPV
            enviarCorreoSolicitud(deviceId2, codigoPV)
        }

        // 4. Botón "Activar licencia" (solo con el código completo)
        binding.btnActivar.setOnClickListener {
            val codigoCompleto = binding.etLicencia.text.toString().trim()
            val posId = binding.etCodigoPuntoVenta.text.toString().trim()
            if (codigoCompleto.isEmpty() || posId.isEmpty()) {
                Toast.makeText(this, "Ingresa la licencia y el código del punto de venta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar formato (debe contener '|')
            if (!codigoCompleto.contains("|")) {
                Toast.makeText(this, "Formato de licencia inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Guardar POS ID en preferencias
            prefs.codigoPuntoVenta = posId

            if (LicenseManager.verifyLicense(this, codigoCompleto, posId)) {
                LicenseManager.saveLicense(this, codigoCompleto,posId )
                Toast.makeText(this, "✅ Licencia activada correctamente", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "❌ Código de licencia inválido o expirado", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Si ya hay licencia válida, pasa directamente a MainActivity
        if (LicenseManager.hasValidLicense(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun enviarCorreoSolicitud(deviceId: String, codigoPV: String) {
        val adminEmail = "yunierlacerdasarria@gmail.com"  // ¡Usa tu correo real!
        val asunto = "Solicitud de licencia - Punto de venta $codigoPV"
        val cuerpo = """
        ID del dispositivo: $deviceId
        Código del punto de venta: $codigoPV
        
        Por favor, genera la licencia para este dispositivo.
        Fecha de expiración sugerida: 1 año a partir de hoy.
    """.trimIndent()

        // Intent con ACTION_SEND (más universal que ACTION_SENDTO)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(adminEmail))
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, cuerpo)
        }

        // Intent con ACTION_SENDTO como fallback (por si el primero falla)
        val sendtoIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(adminEmail))
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, cuerpo)
        }

        // Intent específico para Gmail (opcional, pero útil)
        val gmailIntent = packageManager.getLaunchIntentForPackage("com.google.android.gm")?.let {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = "com.google.android.gm"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(adminEmail))
                putExtra(Intent.EXTRA_SUBJECT, asunto)
                putExtra(Intent.EXTRA_TEXT, cuerpo)
            }
        }

        // Elegir el mejor Intent disponible
        val chosenIntent = when {
            gmailIntent?.resolveActivity(packageManager) != null -> gmailIntent
            intent.resolveActivity(packageManager) != null -> intent
            sendtoIntent.resolveActivity(packageManager) != null -> sendtoIntent
            else -> null
        }

        if (chosenIntent != null) {
            startActivity(Intent.createChooser(chosenIntent, "Enviar solicitud por correo"))
        } else {
            // Si no hay app de correo, copiar al portapapeles y mostrar instrucciones
            copiarAlPortapapeles("$deviceId|$codigoPV")
            // Mostrar un diálogo con instrucciones
            MaterialAlertDialogBuilder(this)
                .setTitle("No hay app de correo")
                .setMessage("""
                No se encontró una aplicación de correo electrónico.
                
                Los datos han sido copiados al portapapeles.
                
                Por favor, envía un correo manualmente a:
                $adminEmail
                
                Incluye el ID del dispositivo y el código del punto de venta.
            """.trimIndent())
                .setPositiveButton("Entendido", null)
                .show()
        }
    }

    private fun copiarAlPortapapeles(texto: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Datos solicitud", texto)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Datos copiados al portapapeles", Toast.LENGTH_SHORT).show()
    }
}