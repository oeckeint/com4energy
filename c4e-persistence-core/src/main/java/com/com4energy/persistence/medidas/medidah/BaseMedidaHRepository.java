package com.com4energy.persistence.medidas.medidah;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseMedidaHRepository extends JpaRepository<MedidaH, Long> {

}
