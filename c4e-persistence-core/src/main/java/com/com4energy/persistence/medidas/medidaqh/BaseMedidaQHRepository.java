package com.com4energy.persistence.medidas.medidaqh;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseMedidaQHRepository extends JpaRepository<MedidaQH, Long> {

}
