package com.com4energy.persistence.clientes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// NOTE: la tabla `cliente` (compartida con records-api/sistemagestion/dashboard) aún no
// tiene columnas de auditoría. Por eso NO extiende Auditable: mapearlas reventaría en runtime.
// Pendiente (future enhancement): añadir created_at/by + updated_at/by a la tabla y restaurar Auditable.
@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente {

    @Id
    @Column(name = "id_cliente")
    private Integer id;

    @Column(name = "cups")
    private String cups;

    @Column(name = "nombre_cliente")
    private String nombreCliente;

    @Column(name = "tarifa")
    private String tarifa;

    @Column(name = "is_deleted")
    private Integer isDeleted;

}
