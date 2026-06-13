package com.com4energy.consumerprobe;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio ficticio de un servicio consumidor, fuera de
 * com.com4energy.persistence. Debe seguir siendo detectado cuando
 * PersistenceCoreAutoConfiguration está activa.
 */
public interface ConsumerProbeRepository extends JpaRepository<ConsumerProbeEntity, Long> {
}
