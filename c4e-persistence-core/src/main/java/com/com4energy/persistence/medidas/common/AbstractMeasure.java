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
    protected Long clienteId;

    @Column(name = "tipo_medida", nullable = false)
    protected Integer tipoMedida;

    @Column(name = "fecha", nullable = false)
    protected LocalDateTime fecha;

    @Column(name = "bandera_inv_ver", nullable = false)
    protected Integer banderaInvVer;

    // Campos INT UNSIGNED en BD -> usar Long para evitar overflow (UNSIGNED max = 4,294,967,295)
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
