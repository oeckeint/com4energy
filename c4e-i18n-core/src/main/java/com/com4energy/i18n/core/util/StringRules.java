package com.com4energy.i18n.core.util;

import java.util.function.Function;

public final class StringRules {

    private StringRules() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isBlank(String value) {
        return trimOrNull(value) == null;
    }

    public static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static <T> String nullSafe(T value, Function<T, String> mapper) {
        if (value == null) {
            return null;
        }
        return mapper.apply(value);
    }
}

