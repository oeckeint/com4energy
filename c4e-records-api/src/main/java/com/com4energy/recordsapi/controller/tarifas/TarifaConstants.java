package com.com4energy.recordsapi.controller.tarifas;

import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.recordsapi.controller.common.ApiConstants;

public final class TarifaConstants {

    public static final String BASE_PATH = ApiConstants.API_V1 + "/tarifas";

    private TarifaConstants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }

}
