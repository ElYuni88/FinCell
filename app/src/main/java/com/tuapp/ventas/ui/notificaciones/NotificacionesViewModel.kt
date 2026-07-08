package com.tuapp.ventas.ui.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.repository.VentasRepository
import kotlinx.coroutines.launch

class NotificacionesViewModel(private val repo: VentasRepository) : ViewModel() {
    val notificaciones = repo.observarNotificaciones().asLiveData()
    fun generar() = viewModelScope.launch { repo.generarNotificacionesSistema() }
    fun eliminar(id: Long) = viewModelScope.launch { repo.eliminarNotificacion(id) }
    fun eliminarSeleccionadas(ids: List<Long>) = viewModelScope.launch { if (ids.isNotEmpty()) repo.eliminarNotificaciones(ids) }
    fun marcarLeidas(ids: List<Long>) = viewModelScope.launch { if (ids.isNotEmpty()) repo.marcarNotificacionesLeidas(ids) }
}

class NotificacionesViewModelFactory(private val repo: VentasRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = NotificacionesViewModel(repo) as T
}
