package com.com4energy.recordsapi.controller.medidas.h;

import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.Constants;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.recordsapi.controller.common.ApiConstants;
import com.com4energy.recordsapi.controller.common.ResponseHelper;
import com.com4energy.recordsapi.controller.common.dto.PageResponse;
import com.com4energy.recordsapi.controller.medidas.DateRangeHelper;
import com.com4energy.recordsapi.controller.medidas.MedidasConstants;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHHourlyPoint;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHCellOriginResponse;
import com.com4energy.recordsapi.domain.entity.medidas.MedidaH;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.service.MedidaHService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping(MedidaHConstants.BASE_PATH)
public class MedidaHController {

    private final MedidaHService medidaHService;

    @GetMapping
    public ResponseEntity<PageResponse<MedidaH>> getAll(
            @RequestParam(name = Constants.ID_CLIENTE, required = false) Integer idCliente,
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

    @GetMapping(MedidaHConstants.LAST_24H_PATH)
    public ResponseEntity<PageResponse<MedidaH>> last24Hours(
            @RequestParam(name = Constants.ID_CLIENTE, required = false) Integer idCliente,
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
    public ResponseEntity<MedidaH> getById(@PathVariable Integer id) {

        MedidaH medida = medidaHService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Messages.format(RecordsApiCommonMessageKey.MEDIDA_NOT_FOUND, id)
                ));

        return ResponseHelper.ok(medida);
    }

    @PostMapping
    public ResponseEntity<MedidaH> save(@Validated @RequestBody MedidaH medidaH) {
        MedidaH saved = medidaHService.save(medidaH);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(ApiConstants.ID_PATH)
                .buildAndExpand(saved.getIdMedidaH())
                .toUri();
        return ResponseHelper.created(location, saved);
    }

    /**
     * Devuelve actent por cliente y hora en formato compacto para la vista de matriz.
     * Una sola petición sin paginación: GROUP BY id_cliente, HOUR(fecha).
     * Opcionalmente filtra por tarifa del cliente y/o por IDs de cliente.
     * Ejemplo: GET /api/v1/medidah/matrix?date=2026-04-01
     * Ejemplo con tarifa: GET /api/v1/medidah/matrix?date=2026-04-01&tarifa=T20
     * Ejemplo con clientes: GET /api/v1/medidah/matrix?date=2026-04-01&clientIds=101&clientIds=102
     */
    @GetMapping(MedidaHConstants.MATRIX_PATH)
    public ResponseEntity<List<MedidaHHourlyPoint>> getMatrix(
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "tarifa", required = false) String tarifa,
            @RequestParam(name = "clientIds", required = false) List<Integer> clientIds) {
        List<MedidaHHourlyPoint> result = medidaHService.findHourlyMatrixFiltered(date, tarifa, clientIds);
        return ResponseEntity.ok(result);
    }

    @GetMapping(MedidaHConstants.CELL_ORIGINS_PATH)
    public ResponseEntity<MedidaHCellOriginResponse> getCellOrigins(
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "clientId") Integer clientId,
            @RequestParam(name = "hour") Integer hour) {
        if (clientId == null || clientId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId debe ser mayor que 0");
        }
        if (hour == null || hour < 0 || hour > 23) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hour debe estar entre 0 y 23");
        }

        MedidaHCellOriginResponse result = medidaHService.findCellOrigins(date, clientId, hour);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<PageResponse<MedidaH>> findMedidas(
            Integer clienteId,
            DateRangeHelper.DateRange dateRange,
            Pageable pageable) {

        Page<MedidaH> result = medidaHService.findAll(
                clienteId,
                dateRange.getStart(),
                dateRange.getEnd(),
                pageable
        );

        return ResponseHelper.page(result);
    }
}

