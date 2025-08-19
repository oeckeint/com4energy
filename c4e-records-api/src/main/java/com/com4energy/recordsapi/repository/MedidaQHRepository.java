package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.dto.MedidaQH;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedidaQHRepository extends JpaRepository<MedidaQH, Integer> {


}
