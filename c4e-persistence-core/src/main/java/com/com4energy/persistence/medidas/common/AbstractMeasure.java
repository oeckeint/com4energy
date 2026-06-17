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

    @Column(name = "tipo_medida", nullable = false)
    protected Integer tipoMedida;

    @Column(name = "fecha", nullable = false)
    protected LocalDateTime fecha;

    @Column(name = "bandera_inv_ver", nullable = false)
    protected Integer banderaInvVer;

    // Magnitudes y códigos de calidad: en BD son SMALLINT UNSIGNED (tope 65535; valores reales < 1k,
    // zona segura ~10k). El tipo Java se mantiene en Long (ddl-auto=none -> sin validación de esquema;
    // el ensanchado al leer es trivial y un valor fuera de rango cae a cuarentena por strict mode).
    @Column(name = "actent", nullable = false)
    protected Long actent;

    @Column(name = "qactent", nullable = false)
    protected Long qactent;

    @Column(name = "actsal", nullable = false)
    protected Long actsal;

    @Column(name = "qactsal", nullable = false)
    protected Long qactsal;

    @Column(name = "r_q1", nullable = false)
    protected Long rq1;

    @Column(name = "qr_q1", nullable = false)
    protected Long qrq1;

    @Column(name = "r_q2", nullable = false)
    protected Long rq2;

    @Column(name = "qr_q2", nullable = false)
    protected Long qrq2;

    @Column(name = "r_q3", nullable = false)
    protected Long rq3;

    @Column(name = "qr_q3", nullable = false)
    protected Long qrq3;

    @Column(name = "r_q4", nullable = false)
    protected Long rq4;

    @Column(name = "qr_q4", nullable = false)
    protected Long qrq4;

    @Column(name = "medres1", nullable = false)
    protected Long medres1;

    @Column(name = "qmedres1", nullable = false)
    protected Long qmedres1;

    @Column(name = "medres2", nullable = false)
    protected Long medres2;

    @Column(name = "qmedres2", nullable = false)
    protected Long qmedres2;

    @Column(name = "metod_obt")
    protected Integer metodObt;

}
