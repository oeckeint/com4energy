package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.service.measure.MeasureRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Path A del ruido de binary-split: el overflow predecible (valor que no cabe en la columna
 * right-sized) se reclasifica como defecto de validación TOLERANTE, ANTES de la BD, en vez de
 * reventar una constraint y disparar el binary-split + el ruido de Hibernate.
 */
class MagnitudeRangeRecordValidatorTest {

    private static final LocalDateTime TS = LocalDateTime.of(2026, 5, 1, 0, 0);
    private static final String CUPS = "ES123456789012345678";

    private final MagnitudeRangeRecordValidator validator = new MagnitudeRangeRecordValidator();

    @Test
    void brokenRuleIsStorageRange() {
        assertEquals("STORAGE_RANGE", validator.brokenRule());
    }

    @Test
    void flagsP1MagnitudeOverSmallintMax() {
        Optional<String> result = validator.validate(hourly(70000d, 1));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("actent"));
        assertTrue(result.get().contains("[0..65535]"));
        assertTrue(result.get().contains("70000"));
    }

    @Test
    void acceptsP1MagnitudeExactlyAtSmallintMax() {
        assertTrue(validator.validate(hourly(65535d, 1)).isEmpty());
    }

    @Test
    void doesNotFlagP1ValueThatRoundsDownToTheMax() {
        // 65535.4 redondea (HALF_EVEN) a 65535 -> cabe; no debe ser falso positivo.
        assertTrue(validator.validate(hourly(65535.4d, 1)).isEmpty());
    }

    @Test
    void flagsP1ValueThatRoundsUpPastTheMax() {
        // 65535.6 redondea a 65536 -> no cabe; coherente con lo que se persistiría.
        assertTrue(validator.validate(hourly(65535.6d, 1)).isPresent());
    }

    @Test
    void flagsMetodObtOverTinyintMax() {
        Optional<String> result = validator.validate(hourly(1d, 300));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("metodObt"));
        assertTrue(result.get().contains("[0..255]"));
        assertTrue(result.get().contains("300"));
    }

    @Test
    void flagsP2MagnitudeOverSmallintMax() {
        Optional<String> result = validator.validate(quarterHourly(70000, 1));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("rQ2"));
        assertTrue(result.get().contains("[0..65535]"));
    }

    @Test
    void acceptsValidRecords() {
        assertTrue(validator.validate(hourly(500d, 1)).isEmpty());
        assertTrue(validator.validate(quarterHourly(500, 1)).isEmpty());
    }

    @Test
    void doesNotSupportNorFlagCch() {
        MeasureRecord.Cch cch = new MeasureRecord.Cch(CUPS, TS, 0, 100, 1, "raw");
        assertFalse(validator.supports(cch));
        assertTrue(validator.validate(cch).isEmpty());
    }

    /** actent y metodObt parametrizados; el resto en cero (válido). */
    private MeasureRecord.Hourly hourly(double actent, int metodObt) {
        return new MeasureRecord.Hourly(
                CUPS, TS, 11,
                0d,            // banderaInvVer
                actent,        // actent
                0d, 0d, 0d,    // qactent, actsal, qactsal
                0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, // rQ1..qrQ4
                0d, 0d, 0d, 0d, // medres1, qmedres1, medres2, qmedres2
                metodObt, 0, "origen",
                "raw");
    }

    /** rQ2 y metodObt parametrizados; el resto en cero (válido). */
    private MeasureRecord.QuarterHourly quarterHourly(int rq2, int metodObt) {
        return new MeasureRecord.QuarterHourly(
                CUPS, TS, 11,
                0,             // banderaInvVer
                0, 0, 0, 0,    // actent, qactent, actsal, qactsal
                0, 0, rq2, 0, 0, 0, 0, 0, // rQ1, qrQ1, rQ2, qrQ2, rQ3, qrQ3, rQ4, qrQ4
                0, 0, 0, 0,    // medres1, qmedres1, medres2, qmedres2
                metodObt, 0, "origen",
                "raw");
    }
}
