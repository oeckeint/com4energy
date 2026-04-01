package com.com4energy.recordsapi.domain.entity.medidas;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medidaqh")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedidaQH {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medidaQH")
    private int idMedidaQH;

    @Column(name = "id_cliente")
    private int id_cliente;

    @Column(name = "tipomed")
    private int tipomed;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "bandera_inv_ver")
    private int bandera_inv_ver;

    @Column(name = "actent")
    private int actent;

    @Column(name = "qactent")
    private int qactent;

    @Column(name = "qactsal")
    private int qactsal;

    @Column(name = "r_q1")
    private int r_q1;

    @Column(name = "qr_q1")
    private int qr_q1;

    @Column(name = "r_q2")
    private int r_q2;

    @Column(name = "qr_q2")
    private int qr_q2;

    @Column(name = "r_q3")
    private int r_q3;

    @Column(name = "qr_q3")
    private int qr_q3;

    @Column(name = "r_q4")
    private int r_q4;

    @Column(name = "qr_q4")
    private int qr_q4;

    @Column(name = "medres1")
    private int medres1;

    @Column(name = "qmedres1")
    private int qmedres1;

    @Column(name = "medres2")
    private int medres2;

    @Column(name = "qmedres2")
    private int qmedres2;

    @Column(name = "metod_obt")
    private int metod_obt;

    @Column(name = "origen")
    private String origen;

    @Column(name = "created_on")
    private LocalDateTime created_on;

    @Column(name = "created_by")
    private String created_by;

    @Column(name = "updated_on")
    private LocalDateTime updated_on;

    @Column(name = "updated_by")
    private String updated_by;

    @Column(name = "temporal")
    private int temporal;
}
