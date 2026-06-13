package com.com4energy.consumerprobe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad ficticia que simula una entidad propia de un servicio consumidor,
 * ubicada FUERA de com.com4energy.persistence.
 */
@Entity
@Table(name = "consumer_probe")
public class ConsumerProbeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
