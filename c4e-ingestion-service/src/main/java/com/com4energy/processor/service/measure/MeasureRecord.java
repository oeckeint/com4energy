package com.com4energy.processor.service.measure;

import com.com4energy.persistence.filerecord.enums.FileType;

import java.time.LocalDateTime;

public sealed interface MeasureRecord permits MeasureRecord.Hourly,
        MeasureRecord.QuarterHourly, MeasureRecord.Cch {

    FileType kind();

    String cups();

    LocalDateTime timestamp();

    String rawLine();

    // Las magnitudes horarias (P1) vienen con decimales en el archivo; se usan double
    // (no float) para no perder precisión en valores grandes. Al persistir se aplica
    // techo (Math.ceil) a las columnas de energía, por especificación del cliente.
    record Hourly(
            String cups,
            LocalDateTime timestamp,
            int tipoMedida,
            double banderaInvVer,
            double actent,
            double qactent,
            double actsal,
            double qactsal,
            double rQ1,
            double qrQ1,
            double rQ2,
            double qrQ2,
            double rQ3,
            double qrQ3,
            double rQ4,
            double qrQ4,
            double medres1,
            double qmedres1,
            double medres2,
            double qmedres2,
            int metodObt,
            int temporal,
            String origen,
            String rawLine
    ) implements MeasureRecord {
        @Override
        public FileType kind() {
            return FileType.MEDIDA_H_P1;
        }
    }

    record QuarterHourly(
            String cups,
            LocalDateTime timestamp,
            int tipoMedida,
            int banderaInvVer,
            int actent,
            int qactent,
            int actsal,
            int qactsal,
            int rQ1,
            int qrQ1,
            int rQ2,
            int qrQ2,
            int rQ3,
            int qrQ3,
            int rQ4,
            int qrQ4,
            int medres1,
            int qmedres1,
            int medres2,
            int qmedres2,
            int metodObt,
            Integer temporal,
            String origen,
            String rawLine
    ) implements MeasureRecord {
        @Override
        public FileType kind() {
            return FileType.MEDIDA_QH_P2;
        }
    }


    record Cch(
            FileType kind,
            String cups,
            LocalDateTime timestamp,
            int banderaInvVer,
            int actent,
            int metod,
            String rawLine
    ) implements MeasureRecord {
        public Cch(
                String cups,
                LocalDateTime timestamp,
                int banderaInvVer,
                int actent,
                int metod,
                String rawLine
        ) {
            this(FileType.MEDIDA_CCH_F5, cups, timestamp, banderaInvVer, actent, metod, rawLine);
        }
    }
}

