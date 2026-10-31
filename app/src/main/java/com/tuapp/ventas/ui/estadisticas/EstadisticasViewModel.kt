package com.tuapp.ventas.ui.estadisticas

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.MutableLiveData
import com.tuapp.ventas.data.model.ExistenciaProducto
import com.tuapp.ventas.data.model.ResumenProducto
import com.tuapp.ventas.data.repository.VentasRepository
import com.tuapp.ventas.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers

class EstadisticasViewModel(
    private val repo: VentasRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = PreferencesManager(context)

    /**
     * ✅ Trigger manual para recargar (tras cerrar/abrir período, tras venta, etc.)
     * Cambiar `_refreshTrigger.value = System.currentTimeMillis()` recarga ambos LiveData.
     */
    private val _refreshTrigger = MutableLiveData(System.currentTimeMillis())
    val refreshTrigger: LiveData<Long> = _refreshTrigger

    val resumenPeriodo: LiveData<List<ResumenProducto>> = _refreshTrigger.switchMap {
        liveData(Dispatchers.IO) { emit(repo.obtenerResumenVentasPeriodo()) }
    }

    val existenciaPeriodo: LiveData<List<ExistenciaProducto>> = _refreshTrigger.switchMap {
        liveData(Dispatchers.IO) { emit(repo.obtenerExistenciaPeriodo()) }
    }

    fun recargar() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    /** Por compatibilidad con llamadas viejas: ignora la fecha y recarga. */
    @Suppress("UNUSED_PARAMETER")
    fun seleccionarFecha(fecha: Long) {
        recargar()
    }

    /** Exponer el inicio del período actual para la UI si lo necesita. */
    fun obtenerInicioPeriodoActual(): Long =
        prefs.obtenerInicioPeriodoVentas().takeIf { it > 0L } ?: System.currentTimeMillis()
}

class EstadisticasViewModelFactory(
    private val repo: VentasRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        EstadisticasViewModel(repo, context) as T
}