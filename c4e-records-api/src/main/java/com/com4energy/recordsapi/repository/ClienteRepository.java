package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.controller.cliente.dto.ClienteInfoDTO;
import com.com4energy.recordsapi.domain.entity.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    @Query("""
            SELECT new com.com4energy.recordsapi.controller.cliente.dto.ClienteInfoDTO(
                c.idCliente,
                c.cups,
                c.nombreCliente,
                c.tarifa,
                c.isDeleted
            )
            FROM Cliente c
            WHERE c.idCliente = :idCliente
            """)
    Optional<ClienteInfoDTO> findClienteInfoById(@Param("idCliente") Integer idCliente);
}
