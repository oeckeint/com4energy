package com.com4energy.processor.service.validation;

import com.com4energy.persistence.filerecord.enums.FailureReason;
import com.com4energy.processor.config.properties.FileUploadProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileExtensionValidatorTest {

    private final FileExtensionValidator validator = new FileExtensionValidator(uploadProperties());

    // ── Literal allow-list ────────────────────────────────────────────────────

    @Test
    void xmlExtension_isAccepted() {
        assertTrue(validate("factura_20260502.xml").isEmpty());
    }

    // ── Single-digit measure-version extensions 0–9 ───────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "P1D_0031_0894_20260502.0",
            "P1D_0031_0894_20260502.3",
            "F5D_0031_0894_20260502.9"
    })
    void singleDigitVersionExtension_isAccepted(String filename) {
        assertTrue(validate(filename).isEmpty());
    }

    @Test
    void doubleVersionExtension_lastSegmentIsSingleDigit_isAccepted() {
        // extractExtension keeps only the last segment ("4"), which is a single digit
        assertTrue(validate("P1D_0031_0894_20260502.3.4").isEmpty());
    }

    // ── Rejected ──────────────────────────────────────────────────────────────

    @Test
    void multiDigitExtension_isRejected() {
        assertEquals(Optional.of(FailureReason.INVALID_EXTENSION), validate("P1D_0031_0894_20260502.10"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "report.txt",
            "data.csv",
            "archive.zip",
            "image.png"
    })
    void unknownExtension_isRejected(String filename) {
        assertEquals(Optional.of(FailureReason.INVALID_EXTENSION), validate(filename));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Optional<FailureReason> validate(String filename) {
        MockMultipartFile file = new MockMultipartFile("file", filename, "text/xml", "x".getBytes());
        return validator.validate(ValidationContext.from(file));
    }

    private static FileUploadProperties uploadProperties() {
        return new FileUploadProperties(
                null, null, null, null, null, null, null, null, null,
                1L,
                List.of("xml"),
                List.of("text/xml")
        );
    }
}
