package com.com4energy.recordsapi.repository;

import com.com4energy.persistence.clientes.BaseClienteRepository;
import com.com4energy.recordsapi.controller.cliente.dto.ClienteInfoDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends BaseClienteRepository {

    @Query("""
            SELECT new com.com4energy.recordsapi.controller.cliente.dto.ClienteInfoDTO(
                c.id,
                c.cups,
                c.nombreCliente,
                c.tarifa,
                c.isDeleted
            )
            FROM Cliente c
            WHERE c.id = :idCliente
            """)
    Optional<ClienteInfoDTO> findClienteInfoById(@Param("idCliente") Integer idCliente);
}
