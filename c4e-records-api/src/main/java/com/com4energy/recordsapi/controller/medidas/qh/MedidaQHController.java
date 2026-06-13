package com.com4energy.recordsapi.controller.medidas.qh;

import com.com4energy.recordsapi.common.Constants;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.controller.common.ApiConstants;
import com.com4energy.recordsapi.controller.common.ResponseHelper;
import com.com4energy.recordsapi.controller.common.dto.PageResponse;
import com.com4energy.recordsapi.controller.medidas.DateRangeHelper;
import com.com4energy.recordsapi.controller.medidas.MedidasConstants;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.service.MedidaQHService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping(MedidaQHConstants.BASE_PATH)
public class MedidaQHController {

    private final MedidaQHService medidaQHService;

    @GetMapping
    public ResponseEntity<PageResponse<MedidaQH>> getAll(
            @RequestParam(name = Constants.ID_CLIENTE, required = false) Long idCliente,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @PageableDefault(
                    size = ApiConstants.DEFAULT_PAGE_SIZE,
                    sort = Constants.FECHA,
                    direction = Sort.Direction.ASC) Pageable pageable) {

        LocalDateTime start = DateRangeHelper.parseDate(startDate, false);
        LocalDateTime end = DateRangeHelper.parseDate(endDate, true);

        DateRangeHelper.DateRange dateRange = DateRangeHelper.applyDefaultWindow(start, end, MedidasConstants.WINDOW_DAYS);

        return findMedidas(idCliente, dateRange, pageable);
    }

    @GetMapping(MedidaQHConstants.LAST_24H_PATH)
    public ResponseEntity<PageResponse<MedidaQH>> last24Hours(
            @RequestParam(name = Constants.ID_CLIENTE, required = false) Long idCliente,
            @PageableDefault(
                    size = ApiConstants.DEFAULT_PAGE_SIZE,
                    sort = Constants.FECHA,
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        DateRangeHelper.DateRange dateRange =
                DateRangeHelper.lastNDays(MedidasConstants.WINDOW_DAYS);

        return findMedidas(idCliente, dateRange, pageable);
    }

    @GetMapping(ApiConstants.ID_PATH)
    public ResponseEntity<MedidaQH> getById(@PathVariable Long id) {

        MedidaQH medida = medidaQHService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Messages.format(RecordsApiCommonMessageKey.MEDIDA_NOT_FOUND, id)
                ));

        return ResponseHelper.ok(medida);
    }

    private ResponseEntity<PageResponse<MedidaQH>> findMedidas(
            Long clienteId,
            DateRangeHelper.DateRange dateRange,
            Pageable pageable) {

        Page<MedidaQH> result = medidaQHService.findAll(
                clienteId,
                dateRange.getStart(),
                dateRange.getEnd(),
                pageable
        );

        return ResponseHelper.page(result);
    }

}
