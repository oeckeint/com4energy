package com.com4energy.recordsapi.common;

public final class CoreConstants {

    public static final String ID_CLIENTE = "idCliente";
    public static final String FECHA = "fecha";

    private CoreConstants() {
        throw new IllegalStateException(Messages.get(MessageKey.UTILITY_CLASS));
    }
}
