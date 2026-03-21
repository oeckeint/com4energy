package com.com4energy.i18n.core;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {

    private static final String BUNDLE_NAME = "messages";
    private static final String UTILITY_CLASS_MESSAGE = "Utility class";
    private static final String MISSING_MESSAGE_KEY_PREFIX = "Missing message key: ";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private Messages() {
        throw new IllegalStateException(UTILITY_CLASS_MESSAGE);
    }

    public static String get(MessageKey key) {
        try {
            return BUNDLE.getString(key.key());
        } catch (MissingResourceException ex) {
            throw new IllegalStateException(MISSING_MESSAGE_KEY_PREFIX + key.key(), ex);
        }
    }

    public static String format(MessageKey key, Object... args) {
        String pattern = get(key);
        return MessageFormat.format(pattern, args);
    }

}
