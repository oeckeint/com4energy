package com.com4energy.processor.config.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener de métricas de sesión de Hibernate que SOLO registra el bloque de
 * "Session Metrics" cuando la sesión realmente realizó operaciones de escritura
 * (flush de entidades o ejecución de batches JDBC).
 *
 * El listener por defecto de Hibernate ({@code StatisticalLoggingSessionEventListener})
 * emite el bloque en cada cierre de sesión, incluso cuando un job programado solo
 * ejecuta un SELECT que no modifica nada ("flushing a total of 0 entities"). Eso
 * genera ruido cíclico. Este listener mantiene exactamente las mismas métricas pero
 * suprime el log cuando no hubo trabajo de persistencia.
 *
 * <p>Se loguea a nivel DEBUG: con el upsert escribiendo cada lote en su propia transacción
 * ({@code REQUIRES_NEW}), un archivo grande genera ~N sesiones (una por lote de 1000) y a INFO
 * inundaría el log. El resumen operativo por archivo lo da el evento {@code MEASURE_FILE_PROCESSED}
 * (counts + persistMs); estas métricas por sesión quedan disponibles activando DEBUG para
 * diagnóstico fino.
 *
 * Registro (application.yml):
 * <pre>
 * spring.jpa.properties:
 *   hibernate.session.events.log: false                 # desactiva el listener por defecto
 *   hibernate.session.events.auto: com.com4energy.processor.config.persistence.ConditionalSessionMetricsListener
 * </pre>
 *
 * Debe tener constructor público sin argumentos: Hibernate lo instancia por reflexión.
 */
public class ConditionalSessionMetricsListener implements org.hibernate.SessionEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ConditionalSessionMetricsListener.class);

    // JDBC connection
    private long jdbcConnectionAcquisitionCount;
    private long jdbcConnectionAcquisitionTime;
    private long jdbcConnectionAcquisitionStart = -1;

    private long jdbcConnectionReleaseCount;
    private long jdbcConnectionReleaseTime;
    private long jdbcConnectionReleaseStart = -1;

    // JDBC statements
    private long jdbcPrepareStatementCount;
    private long jdbcPrepareStatementTime;
    private long jdbcPrepareStatementStart = -1;

    private long jdbcExecuteStatementCount;
    private long jdbcExecuteStatementTime;
    private long jdbcExecuteStatementStart = -1;

    private long jdbcExecuteBatchCount;
    private long jdbcExecuteBatchTime;
    private long jdbcExecuteBatchStart = -1;

    // Flush
    private long flushCount;
    private long flushEntityCount;
    private long flushCollectionCount;
    private long flushTime;
    private long flushStart = -1;

    // Pre-partial flush
    private long prePartialFlushCount;
    private long prePartialFlushTime;
    private long prePartialFlushStart = -1;

    // Partial flush
    private long partialFlushCount;
    private long partialFlushEntityCount;
    private long partialFlushCollectionCount;
    private long partialFlushTime;
    private long partialFlushStart = -1;

    @Override
    public void jdbcConnectionAcquisitionStart() {
        assert jdbcConnectionAcquisitionStart < 0 : "Nested calls to jdbcConnectionAcquisitionStart";
        jdbcConnectionAcquisitionStart = System.nanoTime();
    }

    @Override
    public void jdbcConnectionAcquisitionEnd() {
        assert jdbcConnectionAcquisitionStart > 0 : "jdbcConnectionAcquisitionEnd without start";
        jdbcConnectionAcquisitionCount++;
        jdbcConnectionAcquisitionTime += (System.nanoTime() - jdbcConnectionAcquisitionStart);
        jdbcConnectionAcquisitionStart = -1;
    }

    @Override
    public void jdbcConnectionReleaseStart() {
        assert jdbcConnectionReleaseStart < 0 : "Nested calls to jdbcConnectionReleaseStart";
        jdbcConnectionReleaseStart = System.nanoTime();
    }

    @Override
    public void jdbcConnectionReleaseEnd() {
        assert jdbcConnectionReleaseStart > 0 : "jdbcConnectionReleaseEnd without start";
        jdbcConnectionReleaseCount++;
        jdbcConnectionReleaseTime += (System.nanoTime() - jdbcConnectionReleaseStart);
        jdbcConnectionReleaseStart = -1;
    }

    @Override
    public void jdbcPrepareStatementStart() {
        assert jdbcPrepareStatementStart < 0 : "Nested calls to jdbcPrepareStatementStart";
        jdbcPrepareStatementStart = System.nanoTime();
    }

    @Override
    public void jdbcPrepareStatementEnd() {
        assert jdbcPrepareStatementStart > 0 : "jdbcPrepareStatementEnd without start";
        jdbcPrepareStatementCount++;
        jdbcPrepareStatementTime += (System.nanoTime() - jdbcPrepareStatementStart);
        jdbcPrepareStatementStart = -1;
    }

    @Override
    public void jdbcExecuteStatementStart() {
        assert jdbcExecuteStatementStart < 0 : "Nested calls to jdbcExecuteStatementStart";
        jdbcExecuteStatementStart = System.nanoTime();
    }

    @Override
    public void jdbcExecuteStatementEnd() {
        assert jdbcExecuteStatementStart > 0 : "jdbcExecuteStatementEnd without start";
        jdbcExecuteStatementCount++;
        jdbcExecuteStatementTime += (System.nanoTime() - jdbcExecuteStatementStart);
        jdbcExecuteStatementStart = -1;
    }

    @Override
    public void jdbcExecuteBatchStart() {
        assert jdbcExecuteBatchStart < 0 : "Nested calls to jdbcExecuteBatchStart";
        jdbcExecuteBatchStart = System.nanoTime();
    }

    @Override
    public void jdbcExecuteBatchEnd() {
        assert jdbcExecuteBatchStart > 0 : "jdbcExecuteBatchEnd without start";
        jdbcExecuteBatchCount++;
        jdbcExecuteBatchTime += (System.nanoTime() - jdbcExecuteBatchStart);
        jdbcExecuteBatchStart = -1;
    }

    @Override
    public void flushStart() {
        assert flushStart < 0 : "Nested calls to flushStart";
        flushStart = System.nanoTime();
    }

    @Override
    public void flushEnd(int numberOfEntities, int numberOfCollections) {
        assert flushStart > 0 : "flushEnd without start";
        flushCount++;
        flushEntityCount += numberOfEntities;
        flushCollectionCount += numberOfCollections;
        flushTime += (System.nanoTime() - flushStart);
        flushStart = -1;
    }

    @Override
    public void prePartialFlushStart() {
        assert prePartialFlushStart < 0 : "Nested calls to prePartialFlushStart";
        prePartialFlushStart = System.nanoTime();
    }

    @Override
    public void prePartialFlushEnd() {
        assert prePartialFlushStart > 0 : "prePartialFlushEnd without start";
        prePartialFlushCount++;
        prePartialFlushTime += (System.nanoTime() - prePartialFlushStart);
        prePartialFlushStart = -1;
    }

    @Override
    public void partialFlushStart() {
        assert partialFlushStart < 0 : "Nested calls to partialFlushStart";
        partialFlushStart = System.nanoTime();
    }

    @Override
    public void partialFlushEnd(int numberOfEntities, int numberOfCollections) {
        assert partialFlushStart > 0 : "partialFlushEnd without start";
        partialFlushCount++;
        partialFlushEntityCount += numberOfEntities;
        partialFlushCollectionCount += numberOfCollections;
        partialFlushTime += (System.nanoTime() - partialFlushStart);
        partialFlushStart = -1;
    }

    @Override
    public void end() {
        if (!performedWrites() || !LOG.isDebugEnabled()) {
            return;
        }

        LOG.debug(
                "Session Metrics {\n"
                        + "    {} nanoseconds spent acquiring {} JDBC connections;\n"
                        + "    {} nanoseconds spent releasing {} JDBC connections;\n"
                        + "    {} nanoseconds spent preparing {} JDBC statements;\n"
                        + "    {} nanoseconds spent executing {} JDBC statements;\n"
                        + "    {} nanoseconds spent executing {} JDBC batches;\n"
                        + "    {} nanoseconds spent executing {} pre-partial-flushes;\n"
                        + "    {} nanoseconds spent executing {} partial-flushes (flushing a total of {} entities and {} collections);\n"
                        + "    {} nanoseconds spent executing {} flushes (flushing a total of {} entities and {} collections)\n"
                        + "}",
                jdbcConnectionAcquisitionTime, jdbcConnectionAcquisitionCount,
                jdbcConnectionReleaseTime, jdbcConnectionReleaseCount,
                jdbcPrepareStatementTime, jdbcPrepareStatementCount,
                jdbcExecuteStatementTime, jdbcExecuteStatementCount,
                jdbcExecuteBatchTime, jdbcExecuteBatchCount,
                prePartialFlushTime, prePartialFlushCount,
                partialFlushTime, partialFlushCount, partialFlushEntityCount, partialFlushCollectionCount,
                flushTime, flushCount, flushEntityCount, flushCollectionCount
        );
    }

    /**
     * Una sesión "hizo trabajo" si flusheó entidades/colecciones o ejecutó batches JDBC.
     * Los SELECT de los jobs de polling no flushean nada ni ejecutan batches → se omiten.
     */
    private boolean performedWrites() {
        return jdbcExecuteBatchCount > 0
                || flushEntityCount > 0
                || flushCollectionCount > 0
                || partialFlushEntityCount > 0
                || partialFlushCollectionCount > 0;
    }
}
