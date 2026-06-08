package com.tuapp.ventas.data.model.relaciones

import androidx.room.Embedded
import androidx.room.Relation
import com.tuapp.ventas.data.model.Cliente
import com.tuapp.ventas.data.model.Cuenta

data class ClienteConCuentas(
    @Embedded val cliente: Cliente,
    @Relation(parentColumn = "id", entityColumn = "cliente_id") val cuentas: List<Cuenta>
)
