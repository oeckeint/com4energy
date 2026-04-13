package com.com4energy.recordsapi.common;

import com.com4energy.i18n.core.Messages;

public final class Constants {

    // Field names
    public static final String ID_CLIENTE = "idCliente";
    public static final String FECHA = "fecha";

    // RabbitMQ argument keys
    public static final String RABBITMQ_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String RABBITMQ_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    // RabbitMQ listener ids
    public static final String FILE_RECORD_LISTENER_ID_PREFIX = "file-record-listener-";

    private Constants() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }

}
