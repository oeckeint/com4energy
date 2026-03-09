package com.com4energy.recordsapi.common;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {

    private static final String BUNDLE_NAME = "messages";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private Messages() {
        throw new IllegalStateException(Messages.get(MessageKey.UTILITY_CLASS));
    }

    public static String get(MessageKey key) {
        try {
            return BUNDLE.getString(key.key());
        } catch (MissingResourceException ex) {
            throw new IllegalStateException(
                    "Missing message key: " + key.key(), ex);
        }
    }

    public static String format(MessageKey key, Object... args) {
        String pattern = get(key);
        return MessageFormat.format(pattern, args);
    }
}