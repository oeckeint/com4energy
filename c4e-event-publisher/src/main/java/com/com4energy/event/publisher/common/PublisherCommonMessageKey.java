package com.com4energy.event.publisher.common;

import com.com4energy.i18n.core.MessageKey;

/**
 * Claves i18n compartidas entre todos los publishers.
 * Se usa para mensajes comunes: errores de conexión, validación, timeout, etc.
 */
public enum PublisherCommonMessageKey implements MessageKey {

    // Aquí irán claves compartidas cuando las necesites
    // Por ahora vacío, pero disponible para futuro reuso

    ;

    private final String key;

    PublisherCommonMessageKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}

