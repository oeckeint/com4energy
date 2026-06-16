package com.com4energy.persistence.medidas.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base de las medidas versionadas. NO extiende Auditable a propósito: las medidas
 * son filas de altísimo volumen y su creación/origen/versión se derivan del
 * file_record apuntado por {@link #fileRecordId} (join), evitando ~4 columnas de
 * auditoría por fila y el coste del AuditingEntityListener por entidad.
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class RevisionControlled {

    @Column(name = "id_file_record", nullable = false)
    protected Long fileRecordId;

    // SHA-256 truncado a 8 bytes (BINARY(8) en BD). Cambio de representación: se compara
    // byte a byte (sin colación) y ahorra ~56 bytes/fila frente al hex CHAR(64).
    @Column(name = "payload_hash", nullable = false, length = 8)
    protected byte[] payloadHash;

    @Column(name = "payload_hash_version", nullable = false)
    protected Integer payloadHashVersion;

}
