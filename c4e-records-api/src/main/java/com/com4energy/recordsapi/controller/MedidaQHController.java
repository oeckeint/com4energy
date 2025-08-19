package com.com4energy.recordsapi.controller;

import com.com4energy.recordsapi.dto.MedidaQH;
import com.com4energy.recordsapi.service.MedidaQHService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medidaqh")
public class MedidaQHController {

    private final MedidaQHService medidaQHService;

    @Autowired
    public MedidaQHController(MedidaQHService medidaQHService) {
        this.medidaQHService = medidaQHService;
    }

    @GetMapping
    public List<MedidaQH> getAll() {
        return medidaQHService.findAll();
    }
}
