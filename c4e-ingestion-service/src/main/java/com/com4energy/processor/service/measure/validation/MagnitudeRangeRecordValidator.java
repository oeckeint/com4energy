package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.common.MeasureFieldNames;
import com.com4energy.processor.service.measure.MeasureMagnitudes;
import com.com4energy.processor.service.measure.MeasureRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.com4energy.processor.service.measure.MeasureMagnitudes.SMALLINT_UNSIGNED_MAX;
import static com.com4energy.processor.service.measure.MeasureMagnitudes.TINYINT_UNSIGNED_MAX;

/**
 * Valida que las magnitudes y códigos de calidad quepan en sus columnas right-sized
 * (SMALLINT UNSIGNED, 0..65535) y que {@code metod_obt} quepa en TINYINT UNSIGNED (0..255),
 * usando exactamente la misma conversión que la persistencia (redondeo HALF_EVEN para las
 * magnitudes P1, cast directo para los códigos de calidad).
 *
 * <p>Reclasifica el overflow predecible como un <b>defecto de validación TOLERANTE</b> (reporte
 * limpio, ANTES de la BD) en lugar de dejar que reviente una constraint y dispare el binary-split
 * + el ruido de Hibernate ({@code SqlExceptionHelper} / {@code HHH100503}). Así el binary-split
 * queda como red de último recurso solo para errores de BD genuinamente inesperados.
 *
 * <p>La cota inferior de las magnitudes ya la cubre {@link NonNegativeMagnitudesRecordValidator}
 * (corre antes, {@code @Order(50)}); aquí se valida la cota superior de esas columnas y el rango
 * completo de {@code metod_obt}. CCH queda fuera (tabla legacy, columnas INT).
 */
@Component
@Order(60)
public class MagnitudeRangeRecordValidator implements MeasureRecordValidator {

    @Override
    public String brokenRule() {
        return "STORAGE_RANGE";
    }

    @Override
    public boolean supports(MeasureRecord measureRecord) {
        return measureRecord instanceof MeasureRecord.Hourly
                || measureRecord instanceof MeasureRecord.QuarterHourly;
    }

    @Override
    public Optional<String> validate(MeasureRecord measureRecord) {
        return switch (measureRecord) {
            case MeasureRecord.Hourly hourly -> firstOutOfRange(p1Fields(hourly));
            case MeasureRecord.QuarterHourly quarterHourly -> firstOutOfRange(p2Fields(quarterHourly));
            case MeasureRecord.Cch ignored -> Optional.empty();
        };
    }

    private List<BoundedField> p1Fields(MeasureRecord.Hourly h) {
        List<BoundedField> fields = new ArrayList<>(17);
        // Magnitudes de energía: redondeo HALF_EVEN, igual que la persistencia.
        addSmallint(fields, MeasureFieldNames.ACTENT, MeasureMagnitudes.roundMagnitude(h.actent()));
        addSmallint(fields, MeasureFieldNames.ACTSAL, MeasureMagnitudes.roundMagnitude(h.actsal()));
        addSmallint(fields, MeasureFieldNames.RQ1, MeasureMagnitudes.roundMagnitude(h.rQ1()));
        addSmallint(fields, MeasureFieldNames.RQ2, MeasureMagnitudes.roundMagnitude(h.rQ2()));
        addSmallint(fields, MeasureFieldNames.RQ3, MeasureMagnitudes.roundMagnitude(h.rQ3()));
        addSmallint(fields, MeasureFieldNames.RQ4, MeasureMagnitudes.roundMagnitude(h.rQ4()));
        addSmallint(fields, MeasureFieldNames.MEDRES1, MeasureMagnitudes.roundMagnitude(h.medres1()));
        addSmallint(fields, MeasureFieldNames.MEDRES2, MeasureMagnitudes.roundMagnitude(h.medres2()));
        // Códigos de calidad: cast directo, igual que la persistencia.
        addSmallint(fields, MeasureFieldNames.QACTENT, (int) h.qactent());
        addSmallint(fields, MeasureFieldNames.QACTSAL, (int) h.qactsal());
        addSmallint(fields, MeasureFieldNames.QRQ1, (int) h.qrQ1());
        addSmallint(fields, MeasureFieldNames.QRQ2, (int) h.qrQ2());
        addSmallint(fields, MeasureFieldNames.QRQ3, (int) h.qrQ3());
        addSmallint(fields, MeasureFieldNames.QRQ4, (int) h.qrQ4());
        addSmallint(fields, MeasureFieldNames.QMEDRES1, (int) h.qmedres1());
        addSmallint(fields, MeasureFieldNames.QMEDRES2, (int) h.qmedres2());
        fields.add(new BoundedField(MeasureFieldNames.METOD_OBT, h.metodObt(), 0, TINYINT_UNSIGNED_MAX));
        return fields;
    }

    private List<BoundedField> p2Fields(MeasureRecord.QuarterHourly q) {
        List<BoundedField> fields = new ArrayList<>(17);
        addSmallint(fields, MeasureFieldNames.ACTENT, q.actent());
        addSmallint(fields, MeasureFieldNames.ACTSAL, q.actsal());
        addSmallint(fields, MeasureFieldNames.RQ1, q.rQ1());
        addSmallint(fields, MeasureFieldNames.RQ2, q.rQ2());
        addSmallint(fields, MeasureFieldNames.RQ3, q.rQ3());
        addSmallint(fields, MeasureFieldNames.RQ4, q.rQ4());
        addSmallint(fields, MeasureFieldNames.MEDRES1, q.medres1());
        addSmallint(fields, MeasureFieldNames.MEDRES2, q.medres2());
        addSmallint(fields, MeasureFieldNames.QACTENT, q.qactent());
        addSmallint(fields, MeasureFieldNames.QACTSAL, q.qactsal());
        addSmallint(fields, MeasureFieldNames.QRQ1, q.qrQ1());
        addSmallint(fields, MeasureFieldNames.QRQ2, q.qrQ2());
        addSmallint(fields, MeasureFieldNames.QRQ3, q.qrQ3());
        addSmallint(fields, MeasureFieldNames.QRQ4, q.qrQ4());
        addSmallint(fields, MeasureFieldNames.QMEDRES1, q.qmedres1());
        addSmallint(fields, MeasureFieldNames.QMEDRES2, q.qmedres2());
        fields.add(new BoundedField(MeasureFieldNames.METOD_OBT, q.metodObt(), 0, TINYINT_UNSIGNED_MAX));
        return fields;
    }

    private void addSmallint(List<BoundedField> fields, String name, int value) {
        fields.add(new BoundedField(name, value, 0, SMALLINT_UNSIGNED_MAX));
    }

    private Optional<String> firstOutOfRange(List<BoundedField> fields) {
        for (BoundedField field : fields) {
            if (field.value() < field.min() || field.value() > field.max()) {
                return Optional.of(field.name() + " fuera de rango ["
                        + field.min() + ".." + field.max() + "]: " + field.value());
            }
        }
        return Optional.empty();
    }

    private record BoundedField(String name, int value, int min, int max) {
    }
}
