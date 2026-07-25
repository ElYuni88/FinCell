package com.tuapp.ventas.ui.ipb

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tuapp.ventas.databinding.ActivityAjustarIpbBinding

class AjustarIPBActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAjustarIpbBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAjustarIpbBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnAceptar.setOnClickListener {
            Toast.makeText(this, "Ajustes guardados (placeholder)", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}