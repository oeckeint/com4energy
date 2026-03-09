package com.com4energy.recordsapi.controller.medidas.qh;

import com.com4energy.recordsapi.common.CoreConstants;
import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import com.com4energy.recordsapi.controller.common.ApiConstants;
import com.com4energy.recordsapi.controller.common.ResponseHelper;
import com.com4energy.recordsapi.controller.medidas.DateRangeHelper;
import com.com4energy.recordsapi.controller.medidas.MedidasConstants;
import com.com4energy.recordsapi.dto.MedidaQH;
import com.com4energy.recordsapi.service.MedidaQHService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping(MedidaQHConstants.BASE_PATH)
public class MedidaQHController {

    private final MedidaQHService medidaQHService;

    @GetMapping
    public ResponseEntity<Page<MedidaQH>> getAll(@RequestParam(required = false) Integer clienteId,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @PageableDefault(size = ApiConstants.DEFAULT_PAGE_SIZE, sort = CoreConstants.FECHA, direction = Sort.Direction.ASC) Pageable pageable) {

        // Parse dates with appropriate defaults (start of day for startDate, end of day for endDate)
        LocalDateTime start = DateRangeHelper.parseDate(startDate, false);
        LocalDateTime end = DateRangeHelper.parseDate(endDate, true);

        // Apply sensible time window if only one date bound is provided
        DateRangeHelper.DateRange dateRange = DateRangeHelper.applyDefaultWindow(start, end, MedidasConstants.WINDOW_DAYS);

        return ResponseHelper.ok(
            medidaQHService.findAll(
                clienteId,
                dateRange.getStart(),
                dateRange.getEnd(),
                pageable)
        );
    }

    @GetMapping(MedidaQHConstants.LAST_24H_PATH)
    public Page<MedidaQH> last24Hours(@RequestParam(required = false) Integer clienteId,
                                      @PageableDefault(size = ApiConstants.DEFAULT_PAGE_SIZE, sort = CoreConstants.FECHA, direction = Sort.Direction.ASC) Pageable pageable) {
        DateRangeHelper.DateRange dateRange = DateRangeHelper.lastNDays(MedidasConstants.WINDOW_DAYS);
        return medidaQHService.findAll(clienteId, dateRange.getStart(), dateRange.getEnd(), pageable);
    }

    @GetMapping(ApiConstants.ID_PATH)
    public MedidaQH getById(@PathVariable int id) {
        return medidaQHService.findById(id).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<MedidaQH> save(@Validated @RequestBody MedidaQH medidaQH) {
        MedidaQH saved = medidaQHService.save(medidaQH);
        URI location = URI.create(MedidaQHConstants.BASE_PATH + "/" + saved.getIdMedidaQH());
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping(ApiConstants.TEST_ALL_PATH)
    public ResponseEntity<List<MedidaQH>> getAllForCliente(@RequestParam(name = CoreConstants.ID_CLIENTE) Integer idCliente) {
        List<MedidaQH> result = medidaQHService.findAllForCliente(idCliente);
        return ResponseHelper.ok(result);
    }

}
