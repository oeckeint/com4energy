package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.dto.MedidaQH;
import com.com4energy.recordsapi.repository.MedidaQHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedidaQHService {

    private final MedidaQHRepository medidaQHRepository;

    @Autowired
    public MedidaQHService(MedidaQHRepository medidaQHRepository) {
        this.medidaQHRepository = medidaQHRepository;
    }

    public List<MedidaQH> findAll() {
        return medidaQHRepository.findAll();
    }

    public Optional<MedidaQH> findById(int id) {
        return medidaQHRepository.findById(id);
    }

    public MedidaQH save(MedidaQH medidaQH) {
        return medidaQHRepository.save(medidaQH);
    }

    public void deleteById(int id) {
        medidaQHRepository.deleteById(id);
    }
}
