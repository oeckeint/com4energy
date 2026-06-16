package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Order(200)
@RequiredArgsConstructor
public class ClientExistsRecordValidator implements MeasureRecordValidator {

    private static final int CUPS_PREFIX_LENGTH = 20;

    private enum ClientStatus { OK, NOT_FOUND, DUPLICATE, NO_ID }

    private final ThreadLocal<Map<String, ClientStatus>> statusByPrefix = new ThreadLocal<>();

    private final ClienteRepository clienteRepository;

    @Override
    public String brokenRule() {
        return "CLIENT_EXISTS";
    }

    @Override
    public boolean supports(MeasureRecord measureRecord) {
        return true;
    }

    @Override
    public void beforeBatch(List<MeasureRecord> measureRecords) {
        // Resolución de clientes en UNA sola consulta por lote (antes: N+1 por CUPS distinto).
        Set<String> prefixes = new HashSet<>();
        for (MeasureRecord record : measureRecords) {
            String cups = record.cups();
            if (cups != null && !cups.isBlank()) {
                String prefix = cupsPrefix(cups.trim());
                if (prefix != null) {
                    prefixes.add(prefix);
                }
            }
        }

        Map<String, ClientStatus> resolved = new HashMap<>();
        if (!prefixes.isEmpty()) {
            Map<String, List<ClienteRepository.ClientePrefixView>> matchesByPrefix = new HashMap<>();
            for (ClienteRepository.ClientePrefixView view : clienteRepository.findLookupByCupsPrefixes(prefixes)) {
                matchesByPrefix.computeIfAbsent(view.getCupsPrefix(), key -> new ArrayList<>()).add(view);
            }
            for (String prefix : prefixes) {
                resolved.put(prefix, statusFor(matchesByPrefix.getOrDefault(prefix, List.of())));
            }
        }
        statusByPrefix.set(resolved);
    }

    @Override
    public void afterBatch() {
        statusByPrefix.remove();
    }

    @Override
    public Optional<String> validate(MeasureRecord measureRecord) {
        String cups = measureRecord.cups();
        // Estas incidencias ya las cubren MandatoryFields y CupsFormat.
        if (cups == null || cups.isBlank()) {
            return Optional.empty();
        }

        String normalizedCups = cups.trim();
        String prefix = cupsPrefix(normalizedCups);
        Map<String, ClientStatus> cache = statusByPrefix.get();

        ClientStatus status;
        if (cache != null) {
            status = prefix == null ? ClientStatus.NOT_FOUND : cache.getOrDefault(prefix, ClientStatus.NOT_FOUND);
        } else if (prefix == null) {
            // Sin contexto de lote y CUPS demasiado corto: no puede casar.
            status = ClientStatus.NOT_FOUND;
        } else {
            // Sin contexto de lote (validación de un único registro): consulta puntual.
            status = statusFor(clienteRepository.findLookupByCupsPrefixes(List.of(prefix)));
        }

        return message(status, normalizedCups);
    }

    private ClientStatus statusFor(List<ClienteRepository.ClientePrefixView> matches) {
        if (matches.isEmpty()) {
            return ClientStatus.NOT_FOUND;
        }
        if (matches.size() > 1) {
            return ClientStatus.DUPLICATE;
        }
        return matches.get(0).getId() == null ? ClientStatus.NO_ID : ClientStatus.OK;
    }

    private Optional<String> message(ClientStatus status, String cups) {
        return switch (status) {
            case OK -> Optional.empty();
            case NOT_FOUND -> Optional.of("No se encontró cliente para CUPS " + cups);
            case DUPLICATE -> Optional.of("Se encontró más de un cliente para CUPS " + cups);
            case NO_ID -> Optional.of("El cliente para CUPS " + cups + " no tiene id_cliente");
        };
    }

    private static String cupsPrefix(String cups) {
        return cups.length() >= CUPS_PREFIX_LENGTH ? cups.substring(0, CUPS_PREFIX_LENGTH) : null;
    }
}
