package com.com4energy.persistence.medidas.medidacch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad legacy (tabla medida_cch_legacy). No extiende Auditable porque la tabla
 * usa otras columnas; auditoría híbrida: los timestamps los pone MySQL
 * (insertable/updatable=false) y Spring rellena el "quién".
 */
@Entity
@Table(name = "medida_cch_legacy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MedidaCCH {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medida_cch")
    private Long id;

    @Column(name = "id_cliente", nullable = false)
    private Integer clienteId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "bandera_inv_ver")
    private Integer banderaInvVer;

    @Column(name = "actent")
    private Integer actent;

    @Column(name = "metod")
    private Integer metod;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_on", insertable = false, updatable = false)
    private LocalDateTime updatedOn;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

}
