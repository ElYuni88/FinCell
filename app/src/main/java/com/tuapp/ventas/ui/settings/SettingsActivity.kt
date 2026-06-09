package com.tuapp.ventas.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tuapp.ventas.databinding.ActivitySettingsBinding
import com.tuapp.ventas.utils.PreferencesManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); binding = ActivitySettingsBinding.inflate(layoutInflater); setContentView(binding.root); prefs = PreferencesManager(this); cargar(); binding.btnGuardar.setOnClickListener { guardar(); finish() }; binding.btnVolver.setOnClickListener { finish() } }
    private fun cargar() { binding.switchSonido.isChecked = prefs.sonidoEscaneo; binding.switchVibrar.isChecked = prefs.vibrarEscaneo; binding.switchConfirmar.isChecked = prefs.confirmarCuenta; binding.switchBackup.isChecked = prefs.backupAutomatico; binding.radioRecordar.isChecked = prefs.modoPredeterminado == "RECORDAR"; binding.radioSimple.isChecked = prefs.modoPredeterminado == "SIMPLE"; binding.radioCuenta.isChecked = prefs.modoPredeterminado == "CUENTA" }
    private fun guardar() { prefs.sonidoEscaneo = binding.switchSonido.isChecked; prefs.vibrarEscaneo = binding.switchVibrar.isChecked; prefs.confirmarCuenta = binding.switchConfirmar.isChecked; prefs.backupAutomatico = binding.switchBackup.isChecked; prefs.modoPredeterminado = when { binding.radioSimple.isChecked -> "SIMPLE"; binding.radioCuenta.isChecked -> "CUENTA"; else -> "RECORDAR" } }
}
