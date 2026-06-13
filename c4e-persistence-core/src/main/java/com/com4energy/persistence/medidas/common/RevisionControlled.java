package com.com4energy.persistence.medidas.common;

import com.com4energy.persistence.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class RevisionControlled extends Auditable {

    @Column(name = "id_file_record", nullable = false)
    protected Long fileRecordId;

    @Column(name = "payload_hash", nullable = false)
    protected String payloadHash;

    @Column(name = "payload_hash_version", nullable = false)
    protected Integer payloadHashVersion;

}
