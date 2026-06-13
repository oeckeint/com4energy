package com.com4energy.processor.service.measure.persistence;

import com.com4energy.persistence.medidas.medidacch.MedidaCCH;
import com.com4energy.processor.repository.MedidaCCHRepository;
import com.com4energy.persistence.medidas.medidah.MedidaH;
import com.com4energy.processor.repository.MedidaHRepository;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import com.com4energy.processor.repository.MedidaQHRepository;
import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaMeasurePersistenceAdapter implements MeasurePersistenceContracts.MeasurePersistencePort {

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_PAYLOAD_HASH_VERSION = 1;

    private final MedidaHRepository medidaHRepository;
    private final MedidaQHRepository medidaQHRepository;
    private final MedidaCCHRepository medidaCCHRepository;
    private final ClienteRepository clienteRepository;
    private final EntityManager entityManager;

    // Contexto para tracking durante binary split
    private final ThreadLocal<BatchSplitContext> batchContext = ThreadLocal.withInitial(BatchSplitContext::new);

    @Override
    @Transactional
    public MeasurePersistenceContracts.MeasurePersistenceResult persist(
            MeasurePersistenceContracts.PersistMeasuresCommand command
    ) {
        BatchSplitContext context = batchContext.get();
        context.reset();

        // Evita el autoflush O(n^2): por defecto Hibernate flushea el persistence context
        // ANTES de cada SELECT (p.ej. resolveClient -> findLookupByCups), re-verificando todas
        // las entidades acumuladas. Con COMMIT, el flush ocurre una sola vez al final.
        entityManager.setFlushMode(FlushModeType.COMMIT);

        List<String> errors = new ArrayList<>();
        Map<String, ClienteResolution> clientCache = new HashMap<>();

        List<MedidaH> p1Batch = new ArrayList<>();
        List<MedidaQH> p2Batch = new ArrayList<>();
        List<MedidaCCH> cchBatch = new ArrayList<>();

        int skipped = 0;
        int persisted = 0;

        try {
            for (MeasureRecord measureRecord : command.measureRecords()) {
                ClienteResolution resolution = resolveClient(measureRecord.cups(), clientCache);
                boolean hasInvalidClient = !resolution.valid();

                if (hasInvalidClient) {
                    errors.add(resolution.errorMessage());
                } else {
                    if (measureRecord instanceof MeasureRecord.Hourly hourly) {
                        MedidaH entity = toMedidaH(hourly, resolution.clientId(), command);
                        context.registerSource(entity, measureRecord);
                        p1Batch.add(entity);
                    } else if (measureRecord instanceof MeasureRecord.QuarterHourly quarterHourly) {
                        MedidaQH entity = toMedidaQH(quarterHourly, resolution.clientId(), command);
                        context.registerSource(entity, measureRecord);
                        p2Batch.add(entity);
                    } else if (measureRecord instanceof MeasureRecord.Cch cch) {
                        MedidaCCH entity = toMedidaCch(cch, resolution.clientId());
                        context.registerSource(entity, measureRecord);
                        cchBatch.add(entity);
                    }

                    // Persist in batches to optimize performance while maintaining transactional integrity
                    if (p1Batch.size() + p2Batch.size() + cchBatch.size() >= DEFAULT_BATCH_SIZE) {
                        persisted += flushBatches(p1Batch, p2Batch, cchBatch);
                    }
                }
            }

            // Flush any remaining records
            persisted += flushBatches(p1Batch, p2Batch, cchBatch);

            return new MeasurePersistenceContracts.MeasurePersistenceResult(
                    persisted,
                    errors.size(),
                    skipped,
                    errors,
                    context.getFailedRecords(),
                    null
            );
        } finally {
            context.reset();
            batchContext.remove();
        }
    }

    /**
     * Persiste los lotes acumulados con binary split en caso de error.
     * Si saveAll falla, divide recursivamente el lote hasta aislar registros malos.
     * Acumula los registros que fallan en el contexto de batch.
     *
     * @return número de registros persistidos
     */
    private int flushBatches(
            List<MedidaH> p1Batch,
            List<MedidaQH> p2Batch,
            List<MedidaCCH> cchBatch
    ) {
        int persistedCount = 0;

        if (!p1Batch.isEmpty()) {
            persistedCount += flushWithBinarySplit(p1Batch, medidaHRepository, EntityType.P1_H);
            p1Batch.clear();
        }
        if (!p2Batch.isEmpty()) {
            persistedCount += flushWithBinarySplit(p2Batch, medidaQHRepository, EntityType.P2_QH);
            p2Batch.clear();
        }
        if (!cchBatch.isEmpty()) {
            persistedCount += flushWithBinarySplit(cchBatch, medidaCCHRepository, EntityType.F5_CCH);
            cchBatch.clear();
        }

        return persistedCount;
    }

    /**
     * Flush con binary split: intenta persistir lote.
     * Si falla, divide recursivamente hasta aislar el registro problemático.
     */
    private <T> int flushWithBinarySplit(
            List<T> batch,
            org.springframework.data.jpa.repository.JpaRepository<T, ?> repository,
            EntityType entityType
    ) {
        if (batch.isEmpty()) {
            return 0;
        }

        try {
            repository.saveAll(new ArrayList<>(batch));
            return batch.size();
        } catch (Exception e) {
            log.warn(
                    "Batch flush failed for {} records of type {}. Attempting binary split. Error: {}",
                    batch.size(),
                    entityType,
                    e.getMessage()
            );
            return binarySplitAndPersist(batch, repository, entityType, e.getMessage());
        }
    }

    /**
     * Binary split recursivo: divide lote por mitad y reintenta.
     * Acumula registros que fallan en el contexto para cuarentena.
     */
    private <T> int binarySplitAndPersist(
            List<T> batch,
            org.springframework.data.jpa.repository.JpaRepository<T, ?> repository,
            EntityType entityType,
            String errorMessage
    ) {
        if (batch.isEmpty()) {
            return 0;
        }

        if (batch.size() == 1) {
            // Base case: 1 registro = es el malo
            logFailedRecord(entityType, errorMessage);
            batchContext.get().addFailedEntity(batch.get(0));
            return 0;
        }

        int mid = batch.size() / 2;
        List<T> first = new ArrayList<>(batch.subList(0, mid));
        List<T> second = new ArrayList<>(batch.subList(mid, batch.size()));

        int persistedCount = 0;

        // Reintenta primera mitad
        try {
            repository.saveAll(first);
            persistedCount += first.size();
            log.debug("First half ({} records) persisted successfully", first.size());
        } catch (Exception e) {
            log.debug("First half failed, recursing with {} records", first.size());
            persistedCount += binarySplitAndPersist(first, repository, entityType, e.getMessage());
        }

        // Reintenta segunda mitad
        try {
            repository.saveAll(second);
            persistedCount += second.size();
            log.debug("Second half ({} records) persisted successfully", second.size());
        } catch (Exception e) {
            log.debug("Second half failed, recursing with {} records", second.size());
            persistedCount += binarySplitAndPersist(second, repository, entityType, e.getMessage());
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

    private ClienteResolution resolveClient(String cups, Map<String, ClienteResolution> clientCache) {
        if (cups == null || cups.isBlank()) {
            return ClienteResolution.error("No se puede resolver el cliente porque el CUPS está vacío");
        }

        String normalizedCups = cups.trim();

        return clientCache.computeIfAbsent(normalizedCups, key -> {
            List<ClienteRepository.ClienteLookupView> matches = clienteRepository.findLookupByCups(
                    key,
                    PageRequest.of(0, 2)
            );
            if (matches.isEmpty()) {
                return ClienteResolution.error("No se encontró cliente para CUPS " + key);
            }
            if (matches.size() > 1) {
                return ClienteResolution.error("Se encontró más de un cliente para CUPS " + key);
            }

            ClienteRepository.ClienteLookupView client = matches.get(0);
            if (client.getId() == null) {
                return ClienteResolution.error("El cliente para CUPS " + key + " no tiene id_cliente");
            }
            return ClienteResolution.success(client.getId(), client.getTarifa());
        });
    }

    private MedidaH toMedidaH(
            MeasureRecord.Hourly measure,
            Long clientId,
            MeasurePersistenceContracts.PersistMeasuresCommand command
    ) {
        return MedidaH.builder()
                .clienteId(clientId)
                .tipoMedida(measure.tipoMedida())
                .fecha(measure.timestamp())
                .banderaInvVer((int) measure.banderaInvVer())
                .actent((long) measure.actent())
                .qactent((long) measure.qactent())
                .actsal((long) measure.actsal())
                .qactsal((long) measure.qactsal())
                .rq1((long) measure.rQ1())
                .qrq1((long) measure.qrQ1())
                .rq2((long) measure.rQ2())
                .qrq2((long) measure.qrQ2())
                .rq3((long) measure.rQ3())
                .qrq3((long) measure.qrQ3())
                .rq4((long) measure.rQ4())
                .qrq4((long) measure.qrQ4())
                .medres1((long) measure.medres1())
                .qmedres1((long) measure.qmedres1())
                .medres2((long) measure.medres2())
                .qmedres2((long) measure.qmedres2())
                .metodObt(measure.metodObt())
                .fileRecordId(command.fileRecordId())
                .payloadHash(sha256Hex(measure.rawLine()))
                .payloadHashVersion(DEFAULT_PAYLOAD_HASH_VERSION)
                .build();
    }

    private MedidaQH toMedidaQH(
            MeasureRecord.QuarterHourly measure,
            Long clientId,
            MeasurePersistenceContracts.PersistMeasuresCommand command
    ) {
        return MedidaQH.builder()
                .clienteId(clientId)
                .tipoMedida(measure.tipoMedida())
                .fecha(measure.timestamp())
                .banderaInvVer(measure.banderaInvVer())
                .actent((long) measure.actent())
                .qactent((long) measure.qactent())
                .actsal((long) measure.actsal())
                .qactsal((long) measure.qactsal())
                .rq1((long) measure.rQ1())
                .qrq1((long) measure.qrQ1())
                .rq2((long) measure.rQ2())
                .qrq2((long) measure.qrQ2())
                .rq3((long) measure.rQ3())
                .qrq3((long) measure.qrQ3())
                .rq4((long) measure.rQ4())
                .qrq4((long) measure.qrQ4())
                .medres1((long) measure.medres1())
                .qmedres1((long) measure.qmedres1())
                .medres2((long) measure.medres2())
                .qmedres2((long) measure.qmedres2())
                .metodObt(measure.metodObt())
                .fileRecordId(command.fileRecordId())
                .payloadHash(sha256Hex(measure.rawLine()))
                .payloadHashVersion(DEFAULT_PAYLOAD_HASH_VERSION)
                .build();
    }

    /**
     * Calcula el SHA-256 (64 caracteres hex) de la línea cruda del registro.
     * Sirve como payload_hash provisional para cumplir el contrato de versionado.
     */
    private String sha256Hex(String rawLine) {
        String input = rawLine != null ? rawLine : "";
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en la JVM; no debería ocurrir.
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private MedidaCCH toMedidaCch(MeasureRecord.Cch measure, Long clientId) {
        return MedidaCCH.builder()
                .clienteId(clientId)
                .fecha(measure.timestamp())
                .banderaInvVer(measure.banderaInvVer())
                .actent(measure.actent())
                .metod(measure.metod())
                .build();
    }

    private record ClienteResolution(Long clientId, String tarifa, String errorMessage) {

        static ClienteResolution success(Long clientId, String tarifa) {
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



