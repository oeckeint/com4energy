package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.domain.entity.cliente.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Integer> {

    List<Tarifa> findByStatusOrderByNombreTarifaAsc(String status);

    List<Tarifa> findAllByOrderByNombreTarifaAsc();
}
