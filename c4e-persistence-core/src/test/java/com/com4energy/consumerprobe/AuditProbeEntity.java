package com.com4energy.consumerprobe;

import com.com4energy.persistence.audit.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad de prueba que extiende la base de auditoría real, para verificar que
 * Spring Data JPA rellena los campos de auditoría al persistir.
 */
@Entity
@Table(name = "audit_probe")
public class AuditProbeEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }
}
