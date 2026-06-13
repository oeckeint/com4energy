package com.com4energy.consumerprobe;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditProbeRepository extends JpaRepository<AuditProbeEntity, Long> {
}
