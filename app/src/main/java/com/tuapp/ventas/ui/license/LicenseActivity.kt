package com.tuapp.ventas.ui.license

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tuapp.ventas.R
import com.tuapp.ventas.databinding.ActivityLicenseBinding
import com.tuapp.ventas.databinding.DialogCodigoTransaccionBinding
import com.tuapp.ventas.ui.main.MainActivity
import com.tuapp.ventas.utils.LicenseManager
import com.tuapp.ventas.utils.PreferencesManager

class LicenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLicenseBinding
    private lateinit var prefs: PreferencesManager

    // Opciones de tiempo con precio
    private val opcionesTiempo = listOf(
        TiempoOpcion("15 días (Prueba gratuita)", 15L * 24 * 60 * 60 * 1000, 0.0, true),
        TiempoOpcion("1 mes - $100.00", 30L * 24 * 60 * 60 * 1000, 100.0, false),
        TiempoOpcion("3 meses - $280.00", 90L * 24 * 60 * 60 * 1000, 280.0, false),
        TiempoOpcion("6 meses - $550.00", 180L * 24 * 60 * 60 * 1000, 550.0, false),
        TiempoOpcion("1 año - $900.00", 365L * 24 * 60 * 60 * 1000, 900.0, false)
    )

    private var tiempoSeleccionado: TiempoOpcion = opcionesTiempo.first()

    // ✅ Launcher para solicitar permiso de notificaciones
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "No podrás recibir notificaciones de expiración de licencia",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        // ✅ Solicitar permiso de notificaciones en Android 13+
        solicitarPermisoNotificaciones()

        // Verificar si ya hay licencia válida
        if (LicenseManager.hasValidLicense(this)) {
            irAMain()
            return
        }

        // Cargar licencia guardada (si existe pero expiró)
        val savedLicense = LicenseManager.loadLicense(this)
        if (savedLicense != null) {
            val daysRemaining = LicenseManager.getDaysRemaining(this)
            if (daysRemaining <= 0) {
                Toast.makeText(this, "⚠️ Tu licencia ha expirado. Renueva para continuar.", Toast.LENGTH_LONG).show()
            } else {
                binding.etLicencia.setText(savedLicense)
            }
        }

        configurarSpinnerTiempo()
        configurarBotones()
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun configurarSpinnerTiempo() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            opcionesTiempo.map { it.displayText }
        )
        binding.spinnerTiempo.adapter = adapter
        binding.spinnerTiempo.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                tiempoSeleccionado = opcionesTiempo[position]
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                tiempoSeleccionado = opcionesTiempo.first()
            }
        })
    }

    private fun configurarBotones() {
        binding.btnSolicitarLicencia.setOnClickListener {
            if (tiempoSeleccionado.esGratuito) {
                // ✅ AHORA la prueba gratuita también se solicita (no se activa automáticamente)
                mostrarDialogoSolicitudGratuita()
            } else {
                // Solicitud con pago
                mostrarDialogoCodigoTransaccion()
            }
        }

        binding.btnActivar.setOnClickListener {
            val codigoCompleto = binding.etLicencia.text.toString().trim()
            if (codigoCompleto.isEmpty()) {
                Toast.makeText(this, "Ingresa el código de licencia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (LicenseManager.verifyLicense(this, codigoCompleto)) {
                // ✅ Detectar si es gratuita por el código de transacción
                val esGratuita = codigoCompleto.endsWith("|GRATUITA")
                LicenseManager.saveLicense(this, codigoCompleto, esGratuita)
                Toast.makeText(this, "✅ Licencia activada correctamente", Toast.LENGTH_LONG).show()
                irAMain()
            } else {
                Toast.makeText(this, "❌ Código de licencia inválido o expirado", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogoSolicitudGratuita() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🎁 Prueba gratuita de 15 días")
            .setMessage("""
                Estás solicitando la prueba gratuita de 15 días.
                
                ⚠️ IMPORTANTE: Solo se puede solicitar una vez por dispositivo.
                
                Al continuar, se enviará tu solicitud con el ID del dispositivo.
                El administrador verificará si ya usaste la prueba anteriormente.
                
                ¿Deseas continuar?
            """.trimIndent())
            .setPositiveButton("Solicitar") { _, _ ->
                enviarSolicitudGratuita()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarSolicitudGratuita() {
        val deviceId = LicenseManager.getDeviceId(this)
        val fecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val mensaje = """
            🎁 SOLICITUD DE PRUEBA GRATUITA
            
            🆔 ID del dispositivo: $deviceId
            📋 Tipo: GRATUITA
            ⏱️ Duración: 15 días
            📅 Fecha: $fecha
            
            ---
            Por favor, verifica si este dispositivo ya usó la prueba gratuita.
        """.trimIndent()

        enviarPorWhatsAppOEmail(mensaje, deviceId, esGratuita = true)
    }

    private fun mostrarDialogoCodigoTransaccion() {
        val dialogBinding = DialogCodigoTransaccionBinding.inflate(layoutInflater)
        val montoFormateado = "$${"%.2f".format(tiempoSeleccionado.precio)}"

        dialogBinding.etCodigoTransaccion.hint = "Código de transacción (ej. TX-123ABC)"
        dialogBinding.tvMontoPagar.text = "💰 Monto a pagar: $montoFormateado"
        dialogBinding.tvCodigoReferencia.text = "📌 Código de referencia: 55808823"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Enviar solicitud") { _, _ ->
                val codigo = dialogBinding.etCodigoTransaccion.text?.toString()?.trim().orEmpty()
                if (codigo.isEmpty()) {
                    Toast.makeText(this, "⚠️ El código de transacción es obligatorio", Toast.LENGTH_SHORT).show()
                } else {
                    enviarSolicitudPago(codigo)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialogBinding.etCodigoTransaccion.requestFocus()
        }

        dialog.show()
    }

    private fun enviarSolicitudPago(codigoTransaccion: String) {
        val deviceId = LicenseManager.getDeviceId(this)
        val dias = tiempoSeleccionado.dias
        val precio = tiempoSeleccionado.precio
        val fecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val mensaje = """
            💳 SOLICITUD DE LICENCIA
            
            🆔 ID del dispositivo: $deviceId
            📋 Tipo: PAGADA
            ⏱️ Tiempo solicitado: $dias días
            💰 Monto pagado: $${"%.2f".format(precio)}
            🔑 Código de transacción: $codigoTransaccion
            📅 Fecha: $fecha
            
            ---
            Por favor, genera la licencia para este dispositivo.
        """.trimIndent()

        enviarPorWhatsAppOEmail(mensaje, deviceId, esGratuita = false)
    }

    private fun enviarPorWhatsAppOEmail(mensaje: String, deviceId: String, esGratuita: Boolean) {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, mensaje)
        }

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("yunierlacerdasarria@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Solicitud de licencia - $deviceId")
            putExtra(Intent.EXTRA_TEXT, mensaje)
        }

        val chosenIntent = when {
            whatsappIntent.resolveActivity(packageManager) != null -> whatsappIntent
            emailIntent.resolveActivity(packageManager) != null -> emailIntent
            else -> null
        }

        if (chosenIntent != null) {
            startActivity(Intent.createChooser(chosenIntent, "Enviar solicitud"))
            Toast.makeText(this, "✅ Solicitud enviada correctamente", Toast.LENGTH_SHORT).show()

            MaterialAlertDialogBuilder(this)
                .setTitle("📨 Solicitud enviada")
                .setMessage("""
                    Tu solicitud ha sido enviada.
                    
                    Por favor, espera la respuesta con tu código de licencia.
                    
                    ⏱️ Una vez recibido el código, ingrésalo en el campo "O ingresa tu licencia" y presiona "Activar licencia".
                """.trimIndent())
                .setPositiveButton("Entendido", null)
                .show()
        } else {
            copiarAlPortapapeles(mensaje)
            MaterialAlertDialogBuilder(this)
                .setTitle("No hay app de mensajería")
                .setMessage("""
                    No se encontró WhatsApp ni aplicación de correo.
                    
                    Los datos han sido copiados al portapapeles.
                    
                    Por favor, envía la solicitud manualmente a:
                    📧 yunierlacerdasarria@gmail.com
                """.trimIndent())
                .setPositiveButton("Entendido", null)
                .show()
        }
    }

    private fun copiarAlPortapapeles(texto: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Solicitud licencia", texto)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Datos copiados al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    data class TiempoOpcion(
        val displayText: String,
        val tiempoMs: Long,
        val precio: Double,
        val esGratuito: Boolean = false
    ) {
        val dias: Long get() = tiempoMs / (24 * 60 * 60 * 1000)
    }
}