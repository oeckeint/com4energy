package com.com4energy.recordsapi.controller.medidas.qh;

import com.com4energy.recordsapi.common.CoreConstants;
import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import com.com4energy.recordsapi.controller.common.ApiConstants;
import com.com4energy.recordsapi.controller.medidas.MedidasConstants;
import com.com4energy.recordsapi.dto.MedidaQH;
import com.com4energy.recordsapi.service.MedidaQHService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
    public ResponseEntity<?> getAll(@RequestParam(required = false) Integer clienteId,
                                 @RequestParam(required = false, name = CoreConstants.ID_CLIENTE) Integer idClienteAlias,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @PageableDefault(size = ApiConstants.DEFAULT_PAGE_SIZE, sort = CoreConstants.FECHA, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDateTime start = null;
        LocalDateTime end = null;

        // allow either clienteId or idCliente parameter name
        if (clienteId == null) {
            clienteId = idClienteAlias;
        }

        if (startDate != null && !startDate.isBlank()) {
            try {
                start = OffsetDateTime.parse(startDate).toLocalDateTime();
            } catch (Exception e) {
                // if not offset format, try simple date or local datetime
                if (startDate.length() == MedidasConstants.DATE_ONLY_LENGTH) { // yyyy-MM-dd
                    start = LocalDate.parse(startDate).atStartOfDay();
                } else {
                    start = LocalDateTime.parse(startDate);
                }
            }
        }
        if (endDate != null && !endDate.isBlank()) {
            try {
                end = OffsetDateTime.parse(endDate).toLocalDateTime();
            } catch (Exception e) {
                if (endDate.length() == MedidasConstants.DATE_ONLY_LENGTH) {
                    end = LocalDate.parse(endDate).atTime(
                        MedidasConstants.END_OF_DAY_HOUR,
                        MedidasConstants.END_OF_DAY_MINUTE,
                        MedidasConstants.END_OF_DAY_SECOND,
                        MedidasConstants.END_OF_DAY_NANO
                    );
                } else {
                    end = LocalDateTime.parse(endDate);
                }
            }
        }

        // If only one side provided, make a sensible 24h window
        if (start != null && end == null) {
            end = start.plusDays(MedidasConstants.WINDOW_DAYS);
        }
        if (end != null && start == null) {
            start = end.minusDays(MedidasConstants.WINDOW_DAYS);
        }

        // NO aplicamos ventana de 24h por defecto si no hay fechas, 
        // para que traiga los datos filtrados solo por clienteId si es lo único que se pasa.
        
        Page<MedidaQH> result = medidaQHService.findAll(clienteId, start, end, pageable);
        
        if (result.isEmpty()) {
            // Devolvemos un 200 pero con un mensaje claro en lugar de solo content []
            // Esto ayuda al frontend a saber que no es un error de conexión, sino que no hay datos.
            return ResponseEntity.ok()
                .header(Messages.get(MessageKey.HEADER_INFO), Messages.get(MessageKey.NO_DATA_FOUND_CRITERIA))
                .body(result);
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping(MedidaQHConstants.LAST_24H_PATH)
    public Page<MedidaQH> last24Hours(@RequestParam(required = false) Integer clienteId,
                                      @PageableDefault(size = ApiConstants.DEFAULT_PAGE_SIZE, sort = CoreConstants.FECHA, direction = Sort.Direction.ASC) Pageable pageable) {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDateTime end = now.toLocalDateTime();
        LocalDateTime start = now.minusDays(MedidasConstants.WINDOW_DAYS).toLocalDateTime();
        return medidaQHService.findAll(clienteId, start, end, pageable);
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
    public List<MedidaQH> getAllForCliente(@RequestParam(name = CoreConstants.ID_CLIENTE) Integer idCliente) {
        return medidaQHService.findAllForCliente(idCliente);
    }

}
