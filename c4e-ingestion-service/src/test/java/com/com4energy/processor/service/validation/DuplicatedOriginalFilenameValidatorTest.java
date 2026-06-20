package com.com4energy.processor.service.validation;

import com.com4energy.persistence.filerecord.enums.FailureReason;
import com.com4energy.processor.service.FileRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Guard del chain de subida: rechaza un archivo cuyo nombre ya existe en BD
 * ({@code DUPLICATED_ORIGINAL_FILENAME}) antes de procesar/parsear. Es la primera barrera para el
 * reenvío de la MISMA familia + misma (revisión, iteración) — que es exactamente el mismo nombre.
 */
class DuplicatedOriginalFilenameValidatorTest {

    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private final DuplicatedOriginalFilenameValidator validator =
            new DuplicatedOriginalFilenameValidator(fileRecordService);

    @Test
    void rejectsWhenFilenameAlreadyExists() {
        when(fileRecordService.existsByFilename("P1D_0031_0894_20260502.3")).thenReturn(true);

        assertEquals(
                Optional.of(FailureReason.DUPLICATED_ORIGINAL_FILENAME),
                validator.validate(contextFor("P1D_0031_0894_20260502.3"))
        );
    }

    @Test
    void acceptsWhenFilenameIsNew() {
        when(fileRecordService.existsByFilename("P1D_0031_0894_20260502.4")).thenReturn(false);

        assertTrue(validator.validate(contextFor("P1D_0031_0894_20260502.4")).isEmpty());
    }

    @Test
    void acceptsAndDoesNotHitDbWhenFilenameIsNull() {
        ValidationContext context = ValidationContext.from(
                new MockMultipartFile("file", null, "application/octet-stream", "x".getBytes()));

        assertTrue(validator.validate(context).isEmpty());
        verifyNoInteractions(fileRecordService);
    }

    private ValidationContext contextFor(String filename) {
        return ValidationContext.from(
                new MockMultipartFile("file", filename, "application/octet-stream", "x".getBytes()));
    }
}
