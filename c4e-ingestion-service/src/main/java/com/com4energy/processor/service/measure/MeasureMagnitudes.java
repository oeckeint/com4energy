package com.com4energy.processor.service.measure;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Constantes de rango y redondeo compartidos para las columnas right-sized de medidas.
 *
 * <p>Única fuente de verdad: tanto el adapter de persistencia (al convertir a entidad) como la
 * validación de rango ({@code MagnitudeRangeRecordValidator}) la usan, de modo que el chequeo previo
 * y lo que realmente se persiste coincidan exactamente.
 */
public final class MeasureMagnitudes {

    /** Magnitudes de energía y códigos de calidad: columnas SMALLINT UNSIGNED en BD (0..65535). */
    public static final int SMALLINT_UNSIGNED_MAX = 65_535;

    /** tipo_medida / metod_obt: columnas TINYINT UNSIGNED en BD (0..255). */
    public static final int TINYINT_UNSIGNED_MAX = 255;

    /**
     * Política de redondeo de magnitudes horarias (P1): al entero más cercano con HALF_EVEN
     * (banker's rounding). Centralizada para que el redondeo de persistencia y el chequeo de rango
     * usen exactamente la misma regla. Detalle en {@link #roundMagnitude(double)}.
     */
    public static final RoundingMode MAGNITUDE_ROUNDING = RoundingMode.HALF_EVEN;

    private MeasureMagnitudes() {
    }

    /**
     * Redondea una magnitud horaria (P1) al entero más cercano con {@link #MAGNITUDE_ROUNDING}
     * (HALF_EVEN). Ejemplos: 7.001 -> 7, 7.499 -> 7, 7.999 -> 8; en el empate exacto va al PAR:
     * 0.5 -> 0, 7.5 -> 8, 8.5 -> 8.
     *
     * <p>Se redondea vía {@code BigDecimal.valueOf(double)} (usa la representación decimal corta del
     * double) para que el borde .5 sea EXACTO a los decimales del archivo (≤3) — con {@code double}
     * crudo un 7.5 podría ser 7.4999… y romper el empate.
     */
    public static int roundMagnitude(double value) {
        return BigDecimal.valueOf(value).setScale(0, MAGNITUDE_ROUNDING).intValue();
    }
}
