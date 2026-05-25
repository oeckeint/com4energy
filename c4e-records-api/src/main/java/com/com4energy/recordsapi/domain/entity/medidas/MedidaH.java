package com.com4energy.recordsapi.domain.entity.medidas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "medida_h")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedidaH {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medida_h")
    private Integer idMedidaH;

    @Column(name = "id_cliente", nullable = false)
    private Integer clienteId;

    @Column(name = "tipo_medida")
    private Integer tipoMedida;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "bandera_inv_ver")
    private Integer banderaInvVer;

    @Column(name = "actent")
    private BigDecimal actent;

    @Column(name = "qactent")
    private BigDecimal qactent;

    @Column(name = "actsal")
    private BigDecimal actsal;

    @Column(name = "qactsal")
    private BigDecimal qactsal;

    @Column(name = "r_q1")
    private BigDecimal r_q1;

    @Column(name = "qr_q1")
    private BigDecimal qr_q1;

    @Column(name = "r_q2")
    private BigDecimal r_q2;

    @Column(name = "qr_q2")
    private BigDecimal qr_q2;

    @Column(name = "r_q3")
    private BigDecimal r_q3;

    @Column(name = "qr_q3")
    private BigDecimal qr_q3;

    @Column(name = "r_q4")
    private BigDecimal r_q4;

    @Column(name = "qr_q4")
    private BigDecimal qr_q4;

    @Column(name = "medres1")
    private BigDecimal medres1;

    @Column(name = "qmedres1")
    private BigDecimal qmedres1;

    @Column(name = "medres2")
    private BigDecimal medres2;

    @Column(name = "qmedres2")
    private BigDecimal qmedres2;

    @Column(name = "metod_obt")
    private Integer metodObt;

    @Column(name = "temporal")
    private Integer temporal;

    @Column(name = "origen")
    private String origen;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "updated_by")
    private String updatedBy;
}

