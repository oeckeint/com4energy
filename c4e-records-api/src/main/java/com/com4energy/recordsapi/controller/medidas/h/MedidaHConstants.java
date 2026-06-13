package com.com4energy.recordsapi.controller.medidas.h;

import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.recordsapi.controller.common.ApiConstants;

public final class MedidaHConstants {
    static final String BASE_PATH = ApiConstants.API_V1 + "/medidah";
    static final String LAST_24H_PATH = "/last24h";
    static final String MATRIX_PATH = "/matrix";
    static final String CELL_ORIGINS_PATH = "/cell-origins";

    private MedidaHConstants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }
}

