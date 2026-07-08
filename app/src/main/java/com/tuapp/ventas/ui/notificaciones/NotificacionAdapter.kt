package com.tuapp.ventas.ui.notificaciones

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuapp.ventas.data.model.Notificacion
import com.tuapp.ventas.databinding.ItemNotificacionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificacionAdapter(
    private val onSeleccionCambio: () -> Unit,
    private val onEliminar: (Notificacion) -> Unit
) : ListAdapter<Notificacion, NotificacionAdapter.VH>(Diff) {
    private val seleccionadas = mutableSetOf<Long>()
    private val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun idsSeleccionadas(): List<Long> = seleccionadas.toList()
    fun seleccionarTodas(seleccionar: Boolean) {
        seleccionadas.clear()
        if (seleccionar) seleccionadas.addAll(currentList.map { it.id })
        notifyDataSetChanged()
        onSeleccionCambio()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(ItemNotificacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    override fun onCurrentListChanged(previousList: MutableList<Notificacion>, currentList: MutableList<Notificacion>) {
        seleccionadas.retainAll(currentList.map { it.id }.toSet())
        onSeleccionCambio()
    }

    inner class VH(private val binding: ItemNotificacionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Notificacion) = with(binding) {
            chkNotificacion.setOnCheckedChangeListener(null)
            chkNotificacion.isChecked = seleccionadas.contains(item.id)
            txtMensaje.text = "${item.mensaje}\n${formato.format(Date(item.fechaGeneracion))}"
            chkNotificacion.setOnCheckedChangeListener { _, checked ->
                if (checked) seleccionadas.add(item.id) else seleccionadas.remove(item.id)
                onSeleccionCambio()
            }
            root.setOnClickListener { chkNotificacion.isChecked = !chkNotificacion.isChecked }
            btnEliminar.setOnClickListener { onEliminar(item) }
        }
    }

    object Diff : DiffUtil.ItemCallback<Notificacion>() {
        override fun areItemsTheSame(oldItem: Notificacion, newItem: Notificacion) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Notificacion, newItem: Notificacion) = oldItem == newItem
    }
}
