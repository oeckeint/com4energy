package com.com4energy.recordsapi.common;

import com.com4energy.i18n.core.Messages;

public final class StringRules {

    private StringRules() {
        throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

}

