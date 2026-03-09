package com.com4energy.recordsapi.controller.medidas.qh;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;

public final class MedidaQHConstants {
    static final String BASE_PATH = "/medidaqh";
    static final String LAST_24H_PATH = "/last24h";

    private MedidaQHConstants() {
        throw new IllegalStateException(Messages.get(MessageKey.UTILITY_CLASS));
    }
}
