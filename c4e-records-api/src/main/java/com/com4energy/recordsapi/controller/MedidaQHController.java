package com.com4energy.recordsapi.controller;

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
@RequestMapping("/medidaqh")
public class MedidaQHController {

    private final MedidaQHService medidaQHService;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) Integer clienteId,
                                 @RequestParam(required = false, name = "idCliente") Integer idClienteAlias,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @PageableDefault(size = 24, sort = "fecha", direction = Sort.Direction.ASC) Pageable pageable) {
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
                if (startDate.length() == 10) { // yyyy-MM-dd
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
                if (endDate.length() == 10) {
                    end = LocalDate.parse(endDate).atTime(23, 59, 59, 999999999);
                } else {
                    end = LocalDateTime.parse(endDate);
                }
            }
        }

        // If only one side provided, make a sensible 24h window
        if (start != null && end == null) {
            end = start.plusDays(1);
        }
        if (end != null && start == null) {
            start = end.minusDays(1);
        }

        // NO aplicamos ventana de 24h por defecto si no hay fechas, 
        // para que traiga los datos filtrados solo por clienteId si es lo único que se pasa.
        
        Page<MedidaQH> result = medidaQHService.findAll(clienteId, start, end, pageable);
        
        if (result.isEmpty()) {
            // Devolvemos un 200 pero con un mensaje claro en lugar de solo content []
            // Esto ayuda al frontend a saber que no es un error de conexión, sino que no hay datos.
            return ResponseEntity.ok()
                .header("X-Info-Message", "No data found for the given criteria")
                .body(result);
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/last24h")
    public Page<MedidaQH> last24Hours(@RequestParam(required = false) Integer clienteId,
                                      @PageableDefault(size = 24, sort = "fecha", direction = Sort.Direction.ASC) Pageable pageable) {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDateTime end = now.toLocalDateTime();
        LocalDateTime start = now.minusDays(1).toLocalDateTime();
        return medidaQHService.findAll(clienteId, start, end, pageable);
    }

    @GetMapping("/{id}")
    public MedidaQH getById(@PathVariable int id) {
        return medidaQHService.findById(id).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<MedidaQH> save(@Validated @RequestBody MedidaQH medidaQH) {
        MedidaQH saved = medidaQHService.save(medidaQH);
        URI location = URI.create("/medidaqh/" + saved.getIdMedidaQH());
        return ResponseEntity.created(location).body(saved);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable int id) {
        medidaQHService.deleteById(id);
    }

    @GetMapping("/testall")
    public List<MedidaQH> getAllForCliente(@RequestParam Integer idCliente) {
        return medidaQHService.findAllForCliente(idCliente);
    }

}
