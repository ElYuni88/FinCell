package com.tuapp.ventas.ui.estadisticas

import android.content.Context
import androidx.lifecycle.*
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.utils.FileExporter
import kotlinx.coroutines.launch

class EstadisticasViewModel(private val repo: VentasRepository, private val context: Context) : ViewModel() {
    val totalSimple = repo.totalVentasDirectasHoy().asLiveData()
    val cantidadSimple = repo.cantidadVentasDirectasHoy().asLiveData()
    val totalCuentas = repo.totalCuentasCerradasHoy().asLiveData()
    val cantidadCuentas = repo.cantidadCuentasCerradasHoy().asLiveData()
    val ventasHoy = repo.ventasDirectasHoy().asLiveData()
    val exportacion = MutableLiveData<Result<String>>()
    fun exportar() = viewModelScope.launch { exportacion.value = runCatching { FileExporter(context, repo).exportarVentasDelDia().file.absolutePath } }
}
class EstadisticasViewModelFactory(private val repo: VentasRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = EstadisticasViewModel(repo, context) as T
}
