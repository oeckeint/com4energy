package com.com4energy.i18n.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagesTest {

    private enum TestKey implements MessageKey {
        HELLO("test.hello"),
        UNKNOWN("test.unknown");

        private final String key;

        TestKey(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }
    }

    @Test
    void shouldGetMessageFromBundle() {
        assertEquals("Hello {0}", Messages.get(TestKey.HELLO));
    }

    @Test
    void shouldFormatMessage() {
        assertEquals("Hello Team", Messages.format(TestKey.HELLO, "Team"));
    }

    @Test
    void shouldThrowWhenKeyDoesNotExist() {
        assertThrows(IllegalStateException.class, () -> Messages.get(TestKey.UNKNOWN));
    }
}

