package com.tuapp.ventas.ui.cuenta

import androidx.lifecycle.*
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.launch

class ActiveAccountViewModel(private val repo: VentasRepository, private val cuentaId: Long) : ViewModel() {
    val cuenta = repo.observarCuenta(cuentaId).asLiveData()
    val mensaje = MutableLiveData<String>()
    fun cerrarCuenta(metodoPago: String, observaciones: String?) = viewModelScope.launch { repo.cerrarCuenta(cuentaId, metodoPago, observaciones); mensaje.value = "Cuenta cerrada correctamente" }
}
class ActiveAccountViewModelFactory(private val repo: VentasRepository, private val cuentaId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ActiveAccountViewModel(repo, cuentaId) as T
}
