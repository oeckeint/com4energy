package com.com4energy.i18n.core;

import com.com4energy.i18n.core.util.StringRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StringRulesTest {

    @Test
    void isBlankShouldHandleNullAndWhitespace() {
        assertTrue(StringRules.isBlank(null));
        assertTrue(StringRules.isBlank(""));
        assertTrue(StringRules.isBlank("   "));
        assertFalse(StringRules.isBlank("  abc  "));
    }

    @Test
    void trimToNullShouldTrimAndCollapseBlanks() {
        assertNull(StringRules.trimOrNull(null));
        assertNull(StringRules.trimOrNull("   "));
        assertEquals("abc", StringRules.trimOrNull("  abc  "));
    }
}

