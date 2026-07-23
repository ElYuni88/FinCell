package com.tuapp.ventas.ui.estadisticas

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.tuapp.ventas.data.model.ExistenciaProducto
import com.tuapp.ventas.data.model.ResumenProducto
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.utils.FileExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EstadisticasViewModel(private val repo: VentasRepository, private val context: Context) : ViewModel() {
    private val _fechaSeleccionada = MutableLiveData(System.currentTimeMillis())
    val fechaSeleccionada: LiveData<Long> = _fechaSeleccionada

    val resumenDia: LiveData<List<ResumenProducto>> = _fechaSeleccionada.switchMap { fecha ->
        liveData(Dispatchers.IO) { emit(repo.obtenerResumenVentasDia(fecha)) }
    }

    val existenciaDia: LiveData<List<ExistenciaProducto>> = _fechaSeleccionada.switchMap {
        liveData(Dispatchers.IO) { emit(repo.obtenerExistenciaDia()) }
    }

    val totalSimple = repo.totalVentasDirectasHoy().asLiveData()
    val cantidadSimple = repo.cantidadVentasDirectasHoy().asLiveData()
    val totalCuentas = repo.totalCuentasCerradasHoy().asLiveData()
    val cantidadCuentas = repo.cantidadCuentasCerradasHoy().asLiveData()
    val cantidadCuentasAbiertas = repo.cantidadCuentasAbiertas().asLiveData()
    val productosAgotados = repo.productosAgotados().asLiveData()
    val ventasHoy = repo.ventasDirectasHoy().asLiveData()
    val exportacion = MutableLiveData<Result<Uri>>()

    fun seleccionarFecha(fecha: Long) {
        _fechaSeleccionada.value = fecha
    }

    fun exportar() = viewModelScope.launch {
        exportacion.value = runCatching {
            FileExporter(context, repo).exportarVentasDelDia().uri
        }
    }
}

class EstadisticasViewModelFactory(private val repo: VentasRepository, private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = EstadisticasViewModel(repo, context) as T
}
