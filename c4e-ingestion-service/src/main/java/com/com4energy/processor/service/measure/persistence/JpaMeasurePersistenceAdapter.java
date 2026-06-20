package com.com4energy.processor.service.measure.persistence;

import com.com4energy.persistence.medidas.medidacch.MedidaCCH;
import com.com4energy.processor.repository.MedidaCCHRepository;
import com.com4energy.persistence.medidas.medidah.MedidaH;
import com.com4energy.processor.repository.MedidaHRepository;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import com.com4energy.processor.repository.MedidaQHRepository;
import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.repository.ExistingMeasureView;
import com.com4energy.processor.service.measure.MeasureRecord;
import static com.com4energy.processor.service.measure.MeasureMagnitudes.roundMagnitude;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaMeasurePersistenceAdapter implements MeasurePersistenceContracts.MeasurePersistencePort {

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_PAYLOAD_HASH_VERSION = 1;
    // 8 bytes (64 bits) = BINARY(8) en BD; suficiente para change-detection 1-vs-1.
    private static final int PAYLOAD_HASH_BYTES = 8;
    // Longitud del prefijo de CUPS contra el que se casa cliente.cups.
    private static final int CUPS_PREFIX_LENGTH = 20;

    private final MedidaHRepository medidaHRepository;
    private final MedidaQHRepository medidaQHRepository;
    private final MedidaCCHRepository medidaCCHRepository;
    private final ClienteRepository clienteRepository;
    // Escribe cada lote en su propia transacción (REQUIRES_NEW) -> commit parcial real del binary-split.
    private final MeasureBatchWriter measureBatchWriter;

    // Contexto para tracking durante binary split
    private final ThreadLocal<BatchSplitContext> batchContext = ThreadLocal.withInitial(BatchSplitContext::new);

    // NO es @Transactional: cada lote se escribe vía MeasureBatchWriter (REQUIRES_NEW) con su propia
    // transacción. El prefetch/resolución son lecturas. Así el binary-split logra commit parcial.
    @Override
    public MeasurePersistenceContracts.MeasurePersistenceResult persist(
            MeasurePersistenceContracts.PersistMeasuresCommand command
    ) {
        BatchSplitContext context = batchContext.get();
        context.reset();

        List<String> errors = new ArrayList<>();
        // Resolución de clientes en UNA sola consulta por archivo (antes: N+1 por CUPS distinto).
        Map<String, ClienteResolution> clientsByPrefix = resolveClientsByPrefix(command.measureRecords());

        // Pre-carga del upsert: una consulta por tipo trae las medidas ya existentes para la
        // business key (id_cliente, fecha) presente en el archivo. La decisión insert/omitir/update
        // se hace exacta por par en memoria (maneja fechas repetidas por cliente y fechas nuevas).
        ExistingMeasures existing = preloadExisting(command.measureRecords(), clientsByPrefix);

        // Pre-check cross-familia (antes de escribir): si alguna (cliente, fecha) existente pertenece
        // a OTRA familia, se rechaza el archivo SIN escribir nada (uk_business es global). Se compara
        // la familia REAL de cada fila existente con la del archivo entrante.
        if (hasCrossFamilyCollision(command.measureRecords(), existing, clientsByPrefix, command.sourceFamilyKey())) {
            log.warn(
                    "Rechazando archivo (id={}): colisión cross-familia — (cliente, fecha) ya pertenecen a otra familia",
                    command.fileRecordId()
            );
            return MeasurePersistenceContracts.MeasurePersistenceResult.crossFamilyRejected();
        }

        // Versión del archivo entrante para la precedencia POR FILA (revisión/iteración).
        IncomingVersion incoming = new IncomingVersion(
                command.sourceFamilyKey(), command.revision(), command.processingIteration());

        List<MedidaH> p1Insert = new ArrayList<>();
        List<MedidaH> p1Update = new ArrayList<>();
        List<MedidaQH> p2Insert = new ArrayList<>();
        List<MedidaQH> p2Update = new ArrayList<>();
        List<MedidaCCH> cchInsert = new ArrayList<>();

        int skippedIdentical = 0;
        int skippedStale = 0;
        int inserted = 0;
        int updated = 0;

        try {
            for (MeasureRecord measureRecord : command.measureRecords()) {
                ClienteResolution resolution = resolveClient(measureRecord.cups(), clientsByPrefix);
                if (!resolution.valid()) {
                    errors.add(resolution.errorMessage());
                    continue;
                }

                if (measureRecord instanceof MeasureRecord.Hourly hourly) {
                    MedidaH entity = toMedidaH(hourly, resolution.clientId(), command);
                    BusinessKey key = new BusinessKey(resolution.clientId(), hourly.timestamp());
                    switch (routeUpsert(entity, existing.h().get(key), incoming, measureRecord, context, p1Insert, p1Update)) {
                        case SKIP_IDENTICAL -> skippedIdentical++;
                        case SKIP_STALE -> skippedStale++;
                        default -> { }
                    }
                } else if (measureRecord instanceof MeasureRecord.QuarterHourly quarterHourly) {
                    MedidaQH entity = toMedidaQH(quarterHourly, resolution.clientId(), command);
                    BusinessKey key = new BusinessKey(resolution.clientId(), quarterHourly.timestamp());
                    switch (routeUpsert(entity, existing.qh().get(key), incoming, measureRecord, context, p2Insert, p2Update)) {
                        case SKIP_IDENTICAL -> skippedIdentical++;
                        case SKIP_STALE -> skippedStale++;
                        default -> { }
                    }
                } else if (measureRecord instanceof MeasureRecord.Cch cch) {
                    // CCH (medida_cch_legacy) no tiene payload_hash: insert-only por ahora.
                    // TODO: cuando exista medida_cch (kernel) con payload_hash + uk_business,
                    // aplicar el mismo upsert (preload + omitir/UPDATE in-place) que H/QH.
                    MedidaCCH entity = toMedidaCch(cch, resolution.clientId());
                    context.registerSource(entity, measureRecord);
                    cchInsert.add(entity);
                }

                if (pendingCount(p1Insert, p1Update, p2Insert, p2Update, cchInsert) >= DEFAULT_BATCH_SIZE) {
                    inserted += flushInserts(p1Insert, p2Insert, cchInsert);
                    updated += flushUpdates(p1Update, p2Update);
                }
            }

            inserted += flushInserts(p1Insert, p2Insert, cchInsert);
            updated += flushUpdates(p1Update, p2Update);

            return new MeasurePersistenceContracts.MeasurePersistenceResult(
                    inserted,
                    updated,
                    skippedIdentical,
                    skippedStale,
                    errors.size(),
                    errors,
                    context.getFailedRecords(),
                    null
            );
        } finally {
            context.reset();
            batchContext.remove();
        }
    }

    private enum UpsertDecision {
        INSERT, UPDATE, SKIP_IDENTICAL, SKIP_STALE
    }

    /** Versión (revisión/iteración) del archivo entrante, para la precedencia por fila. */
    private record IncomingVersion(String family, Integer revision, Integer iteration) {
    }

    /**
     * Clasifica una entidad según su business key contra lo ya existente, con precedencia POR FILA:
     * <ul>
     *   <li>sin existente → INSERT;</li>
     *   <li>existente con revisión/iteración estrictamente más reciente → SKIP_STALE (el entrante es
     *       obsoleto; no se pisa el dato más nuevo);</li>
     *   <li>existente igual o más viejo + hash igual → SKIP_IDENTICAL (no hay cambio real);</li>
     *   <li>existente igual o más viejo + hash distinto → UPDATE in-place (mismo id, columnas nuevas).</li>
     * </ul>
     * Las colisiones cross-familia ya se descartaron antes (pre-check), así que aquí se asume misma familia.
     */
    private <T> UpsertDecision routeUpsert(
            T entity,
            ExistingMeasure existing,
            IncomingVersion incoming,
            MeasureRecord sourceRecord,
            BatchSplitContext context,
            List<T> insertBatch,
            List<T> updateBatch
    ) {
        if (existing == null) {
            context.registerSource(entity, sourceRecord);
            insertBatch.add(entity);
            return UpsertDecision.INSERT;
        }
        if (isExistingStrictlyNewer(existing, incoming)) {
            // Lo existente proviene de una revisión/iteración mayor → el entrante es obsoleto → omitir.
            return UpsertDecision.SKIP_STALE;
        }
        if (Arrays.equals(existing.payloadHash(), payloadHashOf(entity))) {
            // hash idéntico → la fila no cambió → omitir (cero escrituras).
            return UpsertDecision.SKIP_IDENTICAL;
        }
        setId(entity, existing.id());
        context.registerSource(entity, sourceRecord);
        updateBatch.add(entity);
        return UpsertDecision.UPDATE;
    }

    /**
     * ¿La fila existente proviene de una (revisión, iteración) ESTRICTAMENTE mayor que la del archivo
     * entrante? Comparación lexicográfica (revisión y luego iteración). Si no hay versión conocida
     * (entrante o existente sin revisión, p.ej. ruta legacy) → false: que decida el hash.
     */
    private boolean isExistingStrictlyNewer(ExistingMeasure existing, IncomingVersion incoming) {
        if (incoming.revision() == null || existing.revision() == null) {
            return false;
        }
        if (!existing.revision().equals(incoming.revision())) {
            return existing.revision() > incoming.revision();
        }
        return orZero(existing.iteration()) > orZero(incoming.iteration());
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Pre-check cross-familia: ¿alguna (cliente, fecha) ya existente pertenece a OTRA familia? Se
     * compara la familia REAL de la fila existente con la del archivo entrante. Si el entrante no
     * declara familia (ruta legacy), no se evalúa.
     */
    private boolean hasCrossFamilyCollision(
            List<MeasureRecord> records,
            ExistingMeasures existing,
            Map<String, ClienteResolution> clientsByPrefix,
            String incomingFamily
    ) {
        if (incomingFamily == null) {
            return false;
        }
        for (MeasureRecord record : records) {
            ClienteResolution resolution = resolveClient(record.cups(), clientsByPrefix);
            if (!resolution.valid()) {
                continue;
            }
            ExistingMeasure match = null;
            if (record instanceof MeasureRecord.Hourly hourly) {
                match = existing.h().get(new BusinessKey(resolution.clientId(), hourly.timestamp()));
            } else if (record instanceof MeasureRecord.QuarterHourly quarterHourly) {
                match = existing.qh().get(new BusinessKey(resolution.clientId(), quarterHourly.timestamp()));
            }
            if (match != null && match.family() != null && !match.family().equals(incomingFamily)) {
                return true;
            }
        }
        return false;
    }

    private byte[] payloadHashOf(Object entity) {
        if (entity instanceof MedidaH medidaH) {
            return medidaH.getPayloadHash();
        }
        if (entity instanceof MedidaQH medidaQH) {
            return medidaQH.getPayloadHash();
        }
        return null;
    }

    private void setId(Object entity, Long id) {
        if (entity instanceof MedidaH medidaH) {
            medidaH.setId(id);
        } else if (entity instanceof MedidaQH medidaQH) {
            medidaQH.setId(id);
        }
    }

    private int pendingCount(List<?>... batches) {
        int total = 0;
        for (List<?> batch : batches) {
            total += batch.size();
        }
        return total;
    }

    /**
     * Pre-carga las medidas existentes (por tipo) para las business keys del archivo.
     * Una sola consulta por tipo: usa uk_*_business + partition pruning por fecha.
     */
    private ExistingMeasures preloadExisting(
            List<MeasureRecord> records,
            Map<String, ClienteResolution> clientsByPrefix
    ) {
        Set<Integer> hClienteIds = new HashSet<>();
        Set<LocalDateTime> hFechas = new HashSet<>();
        Set<Integer> qhClienteIds = new HashSet<>();
        Set<LocalDateTime> qhFechas = new HashSet<>();

        for (MeasureRecord record : records) {
            ClienteResolution resolution = resolveClient(record.cups(), clientsByPrefix);
            if (!resolution.valid()) {
                continue;
            }
            if (record instanceof MeasureRecord.Hourly hourly) {
                hClienteIds.add(resolution.clientId());
                hFechas.add(hourly.timestamp());
            } else if (record instanceof MeasureRecord.QuarterHourly quarterHourly) {
                qhClienteIds.add(resolution.clientId());
                qhFechas.add(quarterHourly.timestamp());
            }
            // CCH: insert-only, no se pre-carga.
        }

        Map<BusinessKey, ExistingMeasure> h = hClienteIds.isEmpty()
                ? Map.of()
                : toExistingMap(medidaHRepository.findExistingByClienteIdsAndFechas(hClienteIds, hFechas));
        Map<BusinessKey, ExistingMeasure> qh = qhClienteIds.isEmpty()
                ? Map.of()
                : toExistingMap(medidaQHRepository.findExistingByClienteIdsAndFechas(qhClienteIds, qhFechas));

        return new ExistingMeasures(h, qh);
    }

    private Map<BusinessKey, ExistingMeasure> toExistingMap(List<ExistingMeasureView> views) {
        Map<BusinessKey, ExistingMeasure> map = new HashMap<>();
        for (ExistingMeasureView view : views) {
            map.put(
                    new BusinessKey(view.getClienteId(), view.getFecha()),
                    new ExistingMeasure(
                            view.getId(),
                            view.getPayloadHash(),
                            view.getSourceFamilyKey(),
                            view.getRevision(),
                            view.getProcessingIteration()
                    )
            );
        }
        return map;
    }

    private int flushInserts(List<MedidaH> p1, List<MedidaQH> p2, List<MedidaCCH> cch) {
        int persistedCount = 0;
        if (!p1.isEmpty()) {
            persistedCount += flushWithBinarySplit(p1, batch -> measureBatchWriter.insertBatch(medidaHRepository, batch), EntityType.P1_H);
            p1.clear();
        }
        if (!p2.isEmpty()) {
            persistedCount += flushWithBinarySplit(p2, batch -> measureBatchWriter.insertBatch(medidaQHRepository, batch), EntityType.P2_QH);
            p2.clear();
        }
        if (!cch.isEmpty()) {
            persistedCount += flushWithBinarySplit(cch, batch -> measureBatchWriter.insertBatch(medidaCCHRepository, batch), EntityType.F5_CCH);
            cch.clear();
        }
        return persistedCount;
    }

    private int flushUpdates(List<MedidaH> p1, List<MedidaQH> p2) {
        int updatedCount = 0;
        if (!p1.isEmpty()) {
            updatedCount += flushWithBinarySplit(p1, batch -> measureBatchWriter.updateBatch(medidaHRepository, batch), EntityType.P1_H);
            p1.clear();
        }
        if (!p2.isEmpty()) {
            updatedCount += flushWithBinarySplit(p2, batch -> measureBatchWriter.updateBatch(medidaQHRepository, batch), EntityType.P2_QH);
            p2.clear();
        }
        return updatedCount;
    }

    /** Estrategia de persistencia de un lote en su propia transacción (insert / update vía MeasureBatchWriter). */
    @FunctionalInterface
    private interface BatchPersister<T> {
        void persist(List<T> batch);
    }

    /**
     * Persiste un lote con binary split: si falla, divide recursivamente hasta aislar el
     * registro problemático y lo manda a cuarentena.
     *
     * @return número de registros persistidos
     */
    private <T> int flushWithBinarySplit(List<T> batch, BatchPersister<T> persister, EntityType entityType) {
        if (batch.isEmpty()) {
            return 0;
        }
        try {
            persister.persist(new ArrayList<>(batch));
            return batch.size();
        } catch (Exception e) {
            log.warn(
                    "Batch flush failed for {} records of type {}. Attempting binary split. Error: {}",
                    batch.size(),
                    entityType,
                    e.getMessage()
            );
            // La transacción REQUIRES_NEW del lote fallido ya se revirtió sola; reintentamos mitades.
            return binarySplitAndPersist(batch, persister, entityType, e.getMessage());
        }
    }

    private <T> int binarySplitAndPersist(
            List<T> batch,
            BatchPersister<T> persister,
            EntityType entityType,
            String errorMessage
    ) {
        if (batch.isEmpty()) {
            return 0;
        }
        if (batch.size() == 1) {
            logFailedRecord(entityType, errorMessage);
            batchContext.get().addFailedEntity(batch.get(0));
            return 0;
        }

        int mid = batch.size() / 2;
        List<T> first = new ArrayList<>(batch.subList(0, mid));
        List<T> second = new ArrayList<>(batch.subList(mid, batch.size()));

        int persistedCount = 0;
        try {
            persister.persist(first);
            persistedCount += first.size();
        } catch (Exception e) {
            persistedCount += binarySplitAndPersist(first, persister, entityType, e.getMessage());
        }
        try {
            persister.persist(second);
            persistedCount += second.size();
        } catch (Exception e) {
            persistedCount += binarySplitAndPersist(second, persister, entityType, e.getMessage());
        }
        return persistedCount;
    }

    private void logFailedRecord(EntityType entityType, String errorMessage) {
        log.warn(
                "Failed to persist record of type {}. Error: {}",
                entityType,
                errorMessage
        );
    }

    enum EntityType {
        P1_H, P2_QH, F5_CCH
    }

    /** Business key de una medida para casar contra lo existente en el upsert. */
    private record BusinessKey(Integer clienteId, LocalDateTime fecha) {
    }

    /**
     * Estado existente de una medida: su id (para UPDATE in-place), su hash (para detectar cambios)
     * y su procedencia (familia + revisión/iteración) para resolver la precedencia por fila y
     * detectar colisiones cross-familia.
     */
    private record ExistingMeasure(Long id, byte[] payloadHash, String family, Integer revision, Integer iteration) {
    }

    /** Mapas de existentes por tipo, indexados por business key. */
    private record ExistingMeasures(
            Map<BusinessKey, ExistingMeasure> h,
            Map<BusinessKey, ExistingMeasure> qh
    ) {
        boolean isEmpty() {
            return h.isEmpty() && qh.isEmpty();
        }
    }

    /**
     * Contexto para tracking de registros fallidos durante binary split.
     */
    static class BatchSplitContext {
        private final List<MeasureRecord> failedRecords = new ArrayList<>();
        private final Map<Object, MeasureRecord> sourceRecords = new IdentityHashMap<>();

        void registerSource(Object entity, MeasureRecord sourceRecord) {
            sourceRecords.put(entity, sourceRecord);
        }

        void addFailedEntity(Object entity) {
            MeasureRecord sourceRecord = sourceRecords.get(entity);
            if (sourceRecord != null) {
                failedRecords.add(sourceRecord);
            }
        }

        List<MeasureRecord> getFailedRecords() {
            return new ArrayList<>(failedRecords);
        }

        void reset() {
            failedRecords.clear();
            sourceRecords.clear();
        }
    }

    private ClienteResolution resolveClient(String cups, Map<String, ClienteResolution> clientsByPrefix) {
        if (cups == null || cups.isBlank()) {
            return ClienteResolution.error("No se puede resolver el cliente porque el CUPS está vacío");
        }

        String normalizedCups = cups.trim();
        String prefix = cupsPrefix(normalizedCups);
        ClienteResolution resolution = prefix == null ? null : clientsByPrefix.get(prefix);

        return resolution != null
                ? resolution
                : ClienteResolution.error("No se encontró cliente para CUPS " + normalizedCups);
    }

    /**
     * Pre-resuelve, en UNA sola consulta, todos los clientes referenciados por el archivo.
     * Reemplaza el patrón N+1 (un SELECT por CUPS distinto). El mapa resultante se indexa
     * por el prefijo de 20 caracteres del CUPS.
     */
    private Map<String, ClienteResolution> resolveClientsByPrefix(List<MeasureRecord> records) {
        Set<String> prefixes = new HashSet<>();
        for (MeasureRecord record : records) {
            String cups = record.cups();
            if (cups != null && !cups.isBlank()) {
                String prefix = cupsPrefix(cups.trim());
                if (prefix != null) {
                    prefixes.add(prefix);
                }
            }
        }
        if (prefixes.isEmpty()) {
            return Map.of();
        }

        Map<String, List<ClienteRepository.ClientePrefixView>> matchesByPrefix = new HashMap<>();
        for (ClienteRepository.ClientePrefixView view : clienteRepository.findLookupByCupsPrefixes(prefixes)) {
            matchesByPrefix.computeIfAbsent(view.getCupsPrefix(), key -> new ArrayList<>()).add(view);
        }

        Map<String, ClienteResolution> resolutions = new HashMap<>();
        for (String prefix : prefixes) {
            List<ClienteRepository.ClientePrefixView> matches = matchesByPrefix.getOrDefault(prefix, List.of());
            if (matches.isEmpty()) {
                continue; // sin coincidencia -> resolveClient devolverá "no encontrado"
            }
            if (matches.size() > 1) {
                resolutions.put(prefix, ClienteResolution.error("Se encontró más de un cliente para CUPS " + prefix));
                continue;
            }
            ClienteRepository.ClientePrefixView client = matches.get(0);
            if (client.getId() == null) {
                resolutions.put(prefix, ClienteResolution.error("El cliente para CUPS " + prefix + " no tiene id_cliente"));
                continue;
            }
            resolutions.put(prefix, ClienteResolution.success(client.getId(), client.getTarifa()));
        }
        return resolutions;
    }

    /** Prefijo de 20 caracteres del CUPS usado para casar contra cliente.cups; null si es más corto. */
    private static String cupsPrefix(String cups) {
        return cups.length() >= CUPS_PREFIX_LENGTH ? cups.substring(0, CUPS_PREFIX_LENGTH) : null;
    }

    private MedidaH toMedidaH(
            MeasureRecord.Hourly measure,
            Integer clientId,
            MeasurePersistenceContracts.PersistMeasuresCommand command
    ) {
        return MedidaH.builder()
                .clienteId(clientId)
                .tipoMedida((short) measure.tipoMedida())
                .fecha(measure.timestamp())
                // Magnitudes de energía: redondeo al entero más cercano con HALF_EVEN (ver roundMagnitude),
                // porque se suman para compra de energía y conviene un agregado insesgado.
                // q* son códigos enteros: cast directo. banderaInvVer es booleano: 0 -> false, !=0 -> true.
                .banderaInvVer(measure.banderaInvVer() != 0)
                .actent(roundMagnitude(measure.actent()))
                .qactent((int) measure.qactent())
                .actsal(roundMagnitude(measure.actsal()))
                .qactsal((int) measure.qactsal())
                .rq1(roundMagnitude(measure.rQ1()))
                .qrq1((int) measure.qrQ1())
                .rq2(roundMagnitude(measure.rQ2()))
                .qrq2((int) measure.qrQ2())
                .rq3(roundMagnitude(measure.rQ3()))
                .qrq3((int) measure.qrQ3())
                .rq4(roundMagnitude(measure.rQ4()))
                .qrq4((int) measure.qrQ4())
                .medres1(roundMagnitude(measure.medres1()))
                .qmedres1((int) measure.qmedres1())
                .medres2(roundMagnitude(measure.medres2()))
                .qmedres2((int) measure.qmedres2())
                .metodObt((short) measure.metodObt())
                .fileRecordId(command.fileRecordId())
                .payloadHash(computePayloadHash(measure.rawLine()))
                .payloadHashVersion(DEFAULT_PAYLOAD_HASH_VERSION)
                .build();
    }

    private MedidaQH toMedidaQH(
            MeasureRecord.QuarterHourly measure,
            Integer clientId,
            MeasurePersistenceContracts.PersistMeasuresCommand command
    ) {
        return MedidaQH.builder()
                .clienteId(clientId)
                .tipoMedida((short) measure.tipoMedida())
                .fecha(measure.timestamp())
                .banderaInvVer(measure.banderaInvVer() != 0)
                .actent(measure.actent())
                .qactent(measure.qactent())
                .actsal(measure.actsal())
                .qactsal(measure.qactsal())
                .rq1(measure.rQ1())
                .qrq1(measure.qrQ1())
                .rq2(measure.rQ2())
                .qrq2(measure.qrQ2())
                .rq3(measure.rQ3())
                .qrq3(measure.qrQ3())
                .rq4(measure.rQ4())
                .qrq4(measure.qrQ4())
                .medres1(measure.medres1())
                .qmedres1(measure.qmedres1())
                .medres2(measure.medres2())
                .qmedres2(measure.qmedres2())
                .metodObt((short) measure.metodObt())
                .fileRecordId(command.fileRecordId())
                .payloadHash(computePayloadHash(measure.rawLine()))
                .payloadHashVersion(DEFAULT_PAYLOAD_HASH_VERSION)
                .build();
    }

    /**
     * Calcula el payload_hash: SHA-256 de la línea cruda truncado a los primeros
     * {@link #PAYLOAD_HASH_BYTES} bytes (BINARY(8) en BD).
     *
     * <p>Es un hash de change-detection, NO de seguridad: solo se compara 1-vs-1
     * contra el hash de la fila existente con la misma business key (id_cliente,
     * fecha) para decidir omitir o reemplazar. 64 bits son de sobra para ese uso.
     */
    private byte[] computePayloadHash(String rawLine) {
        String input = rawLine != null ? rawLine : "";
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Arrays.copyOf(digest, PAYLOAD_HASH_BYTES);
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en la JVM; no debería ocurrir.
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private MedidaCCH toMedidaCch(MeasureRecord.Cch measure, Integer clientId) {
        return MedidaCCH.builder()
                .clienteId(clientId)
                .fecha(measure.timestamp())
                .banderaInvVer(measure.banderaInvVer())
                .actent(measure.actent())
                .metod(measure.metod())
                .build();
    }

    private record ClienteResolution(Integer clientId, String tarifa, String errorMessage) {

        static ClienteResolution success(Integer clientId, String tarifa) {
            return new ClienteResolution(clientId, tarifa, null);
        }

        static ClienteResolution error(String message) {
            return new ClienteResolution(null, null, message);
        }

        boolean valid() {
            return clientId != null && (errorMessage == null || errorMessage.isBlank());
        }
    }
}
