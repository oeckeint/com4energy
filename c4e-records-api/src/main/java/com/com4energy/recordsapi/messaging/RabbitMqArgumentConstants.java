package com.com4energy.recordsapi.messaging;

import com.com4energy.recordsapi.common.Constants;

/** Claves de argumentos AMQP compartidas para declarar colas con DLX. */
@Deprecated(forRemoval = true)
public final class RabbitMqArgumentConstants {

    private RabbitMqArgumentConstants() {
    }

    public static final String DEAD_LETTER_EXCHANGE = Constants.RABBITMQ_DEAD_LETTER_EXCHANGE;
    public static final String DEAD_LETTER_ROUTING_KEY = Constants.RABBITMQ_DEAD_LETTER_ROUTING_KEY;

}
