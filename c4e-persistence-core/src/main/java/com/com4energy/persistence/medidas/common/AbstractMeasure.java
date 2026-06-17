package com.com4energy.persistence.medidas.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractMeasure extends RevisionControlled {

    @Column(name = "id_cliente", nullable = false)
    protected Integer clienteId;

    // tipo_medida y metod_obt: TINYINT UNSIGNED en BD (valores < 100) -> Short.
    @Column(name = "tipo_medida", nullable = false)
    protected Short tipoMedida;

    @Column(name = "fecha", nullable = false)
    protected LocalDateTime fecha;

    // bandera_inv_ver: booleano (TINYINT(1) en BD, 0/1). Boolean de raíz para un contrato correcto;
    // records-api lo serializa como true/false. (sistemagestion usa su propia entidad legacy, no esta.)
    @Column(name = "bandera_inv_ver", nullable = false)
    protected Boolean banderaInvVer;

    // Magnitudes y códigos de calidad: en BD son SMALLINT UNSIGNED (tope 65535; valores reales < 1k,
    // zona segura ~10k) -> Integer. Un valor fuera de rango cae a cuarentena por strict mode.
    @Column(name = "actent", nullable = false)
    protected Integer actent;

    @Column(name = "qactent", nullable = false)
    protected Integer qactent;

    @Column(name = "actsal", nullable = false)
    protected Integer actsal;

    @Column(name = "qactsal", nullable = false)
    protected Integer qactsal;

    @Column(name = "r_q1", nullable = false)
    protected Integer rq1;

    @Column(name = "qr_q1", nullable = false)
    protected Integer qrq1;

    @Column(name = "r_q2", nullable = false)
    protected Integer rq2;

    @Column(name = "qr_q2", nullable = false)
    protected Integer qrq2;

    @Column(name = "r_q3", nullable = false)
    protected Integer rq3;

    @Column(name = "qr_q3", nullable = false)
    protected Integer qrq3;

    @Column(name = "r_q4", nullable = false)
    protected Integer rq4;

    @Column(name = "qr_q4", nullable = false)
    protected Integer qrq4;

    @Column(name = "medres1", nullable = false)
    protected Integer medres1;

    @Column(name = "qmedres1", nullable = false)
    protected Integer qmedres1;

    @Column(name = "medres2", nullable = false)
    protected Integer medres2;

    @Column(name = "qmedres2", nullable = false)
    protected Integer qmedres2;

    @Column(name = "metod_obt")
    protected Short metodObt;

}
