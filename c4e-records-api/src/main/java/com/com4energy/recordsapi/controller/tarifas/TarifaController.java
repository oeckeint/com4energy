package com.com4energy.recordsapi.controller.tarifas;

import com.com4energy.recordsapi.controller.common.ResponseHelper;
import com.com4energy.recordsapi.service.TarifaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(TarifaConstants.BASE_PATH)
public class TarifaController {

    private final TarifaService tarifaService;

    @GetMapping
    public ResponseEntity<List<String>> getAll() {
        return ResponseHelper.ok(tarifaService.findAvailableTarifas());
    }

}
