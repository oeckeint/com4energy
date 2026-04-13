package com.com4energy.i18n.core.util;

import java.util.List;

public final class CollectionRules {

    private CollectionRules() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> List<T> emptyList() {
        return List.of();
    }

    public static <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public static <T> List<T> defaultIfNullOrEmpty(List<T> values, T fallbackValue) {
        if (values == null || values.isEmpty()) {
            return List.of(fallbackValue);
        }
        return List.copyOf(values);
    }

    public static <T> boolean isNullOrEmpty(List<T> values) {
        return values == null || values.isEmpty();
    }

    public static <T> boolean isEmpty(List<T> values) {
        return isNullOrEmpty(values);
    }

}
