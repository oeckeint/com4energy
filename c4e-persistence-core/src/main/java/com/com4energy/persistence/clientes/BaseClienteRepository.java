package com.com4energy.persistence.clientes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseClienteRepository extends JpaRepository<Cliente, Long> {

}
