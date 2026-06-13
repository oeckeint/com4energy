package com.com4energy.processor.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FileNameVersionParserUtilTest {

    // ── Inserción normal: extensión simple = revision ─────────────────────────

    @Test
    void singleExtension_parsesRevisionAndDefaultIteration() {
        var result = FileNameVersionParserUtil.parse("P1D_0031_0894_20260502.3");

        assertEquals("P1D_0031_0894_20260502", result.sourceFamilyKey());
        assertEquals(3, result.revision());
        assertEquals(0, result.processingIteration());
    }

    @Test
    void singleExtension_zero_isValid() {
        var result = FileNameVersionParserUtil.parse("P2D_0031_0894_20260502.0");

        assertEquals("P2D_0031_0894_20260502", result.sourceFamilyKey());
        assertEquals(0, result.revision());
        assertEquals(0, result.processingIteration());
    }

    @Test
    void singleExtension_nine_isValid() {
        var result = FileNameVersionParserUtil.parse("F5D_0031_0894_20260502.9");

        assertEquals("F5D_0031_0894_20260502", result.sourceFamilyKey());
        assertEquals(9, result.revision());
        assertEquals(0, result.processingIteration());
    }

    // ── Doble extensión: revision + iteration ────────────────────────────────

    @Test
    void doubleExtension_parsesRevisionAndIteration() {
        var result = FileNameVersionParserUtil.parse("P1D_0031_0894_20260502.3.4");

        assertEquals("P1D_0031_0894_20260502", result.sourceFamilyKey());
        assertEquals(3, result.revision());
        assertEquals(4, result.processingIteration());
    }

    @Test
    void doubleExtension_iterationZero_isValid() {
        var result = FileNameVersionParserUtil.parse("P1D_0031_0894_20260502.1.0");

        assertEquals("P1D_0031_0894_20260502", result.sourceFamilyKey());
        assertEquals(1, result.revision());
        assertEquals(0, result.processingIteration());
    }

    // ── Casos que NO son archivos de medidas ─────────────────────────────────

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void nullOrBlank_returnsEmpty(String filename) {
        var result = FileNameVersionParserUtil.parse(filename);

        assertNull(result.sourceFamilyKey());
        assertNull(result.revision());
        assertNull(result.processingIteration());
    }

    @Test
    void xmlExtension_returnsEmpty() {
        var result = FileNameVersionParserUtil.parse("factura_20260502.xml");

        assertNull(result.sourceFamilyKey());
        assertNull(result.revision());
        assertNull(result.processingIteration());
    }

    @Test
    void noExtension_returnsEmpty() {
        var result = FileNameVersionParserUtil.parse("P1D_0031_0894_20260502");

        assertNull(result.sourceFamilyKey());
        assertNull(result.revision());
        assertNull(result.processingIteration());
    }

    @Test
    void multiDigitExtension_isNotVersion() {
        // ".10" no es una extensión de versión válida (solo un dígito es válido)
        var result = FileNameVersionParserUtil.parse("P1D_0031_0894_20260502.10");

        assertNull(result.sourceFamilyKey());
        assertNull(result.revision());
        assertNull(result.processingIteration());
    }

    @Test
    void tripleExtension_middleIsNonDigit_parsesLastTwoAsRevisionIteration() {
        // archivo.abc.3.4 → midExt de "archivo.abc.3" es "3" (dígito), así que:
        // iteration=4, revision=3, family="archivo.abc"  — comportamiento definido
        var result = FileNameVersionParserUtil.parse("archivo.abc.3.4");

        assertEquals("archivo.abc", result.sourceFamilyKey());
        assertEquals(3, result.revision());
        assertEquals(4, result.processingIteration());
    }
}
