package com.tuapp.ventas.data.model.relaciones

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.tuapp.ventas.data.model.Cliente
import com.tuapp.ventas.data.model.Cuenta
import com.tuapp.ventas.data.model.DetalleCuenta
import com.tuapp.ventas.data.model.Producto

data class DetalleConProducto(
    @Embedded val detalle: DetalleCuenta,
    @Relation(parentColumn = "producto_id", entityColumn = "id") val producto: Producto
)

data class CuentaConDetalles(
    @Embedded val cuenta: Cuenta,
    @Relation(parentColumn = "cliente_id", entityColumn = "id") val cliente: Cliente,
    @Relation(entity = DetalleCuenta::class, parentColumn = "id", entityColumn = "cuenta_id") val detalles: List<DetalleConProducto>
)
