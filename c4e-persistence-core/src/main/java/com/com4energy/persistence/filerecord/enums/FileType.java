package com.com4energy.persistence.filerecord.enums;

public enum FileType {
    MEDIDA_CCH_F5,
    MEDIDA_H_P1,
    MEDIDA_QH_P2,
    AWAITING_CLASSIFICATION,
    H_MEASURE,
    FACTURA,
    OTRA_FACTURA,
    DOCUMENTO_PDF,
    UNKNOWN;

    @Override
    public String toString() {
        return name().replace("_", " ");
    }
}
