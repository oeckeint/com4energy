package com.com4energy.persistence.medidas.medidaqh;

import com.com4energy.persistence.medidas.common.AbstractMeasure;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "medida_qh")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MedidaQH extends AbstractMeasure {

    // TSID generado en la aplicación (antes del INSERT) para habilitar JDBC batching.
    // IDENTITY forzaba un round-trip por fila y deshabilitaba el batch.
    @Id
    @Tsid
    @Column(name = "id_medida_qh")
    private Long id;

}
