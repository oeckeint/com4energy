package com.com4energy.recordsapi.common;

import com.com4energy.i18n.core.Messages;

public final class Constants {

    // Field names
    public static final String ID_CLIENTE = "idCliente";
    public static final String FECHA = "fecha";

    private Constants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }
}
