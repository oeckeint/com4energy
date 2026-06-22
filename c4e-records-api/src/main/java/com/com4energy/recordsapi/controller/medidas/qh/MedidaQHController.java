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
import com.com4energy.recordsapi.controller.medidas.dto.MeasureCellOriginResponse;
import com.com4energy.recordsapi.controller.medidas.qh.dto.MedidaQHQuarterHourPoint;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.service.MedidaQHService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<MedidaQH> getById(@PathVariable(name = "id") Long id) {

        MedidaQH medida = medidaQHService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Messages.format(RecordsApiCommonMessageKey.MEDIDA_NOT_FOUND, id)
                ));

        return ResponseHelper.ok(medida);
    }

    /**
     * Matriz cuarto-horaria (96 slots) de actent por cliente y slot, agregada en BD.
     * Una sola petición sin paginación. Opcionalmente filtra por tarifa y/o IDs de cliente.
     */
    @GetMapping(MedidaQHConstants.MATRIX_PATH)
    public ResponseEntity<List<MedidaQHQuarterHourPoint>> getMatrix(
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "tarifa", required = false) String tarifa,
            @RequestParam(name = "clientIds", required = false) List<Integer> clientIds) {
        List<MedidaQHQuarterHourPoint> result = medidaQHService.findQuarterHourMatrixFiltered(date, tarifa, clientIds);
        return ResponseEntity.ok(result);
    }

    /**
     * Detalle de orígenes de una celda de la matriz QH: qué archivo(s) de carga aportan
     * registros a un (cliente, hora, minuto) del día indicado.
     */
    @GetMapping(MedidaQHConstants.CELL_ORIGINS_PATH)
    public ResponseEntity<MeasureCellOriginResponse> getCellOrigins(
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "clientId") Integer clientId,
            @RequestParam(name = "hour") Integer hour,
            @RequestParam(name = "minute") Integer minute) {
        if (clientId == null || clientId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId debe ser mayor que 0");
        }
        if (hour == null || hour < 0 || hour > 23) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hour debe estar entre 0 y 23");
        }
        if (minute == null || (minute != 0 && minute != 15 && minute != 30 && minute != 45)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minute debe ser 0, 15, 30 o 45");
        }

        MeasureCellOriginResponse result = medidaQHService.findCellOrigins(date, clientId, hour, minute);
        return ResponseEntity.ok(result);
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
