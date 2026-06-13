package com.com4energy.recordsapi.controller.medidas.cch;

import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.recordsapi.controller.common.ApiConstants;

public final class MedidaCCHConstants {
    static final String BASE_PATH = ApiConstants.API_V1 + "/medidacch";
    static final String LAST_24H_PATH = "/last24h";

    private MedidaCCHConstants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }
}

