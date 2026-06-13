package com.com4energy.persistence.medidas.medidacch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseMedidaCCHRepository extends JpaRepository<MedidaCCH, Long> {

}
