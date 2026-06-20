package com.com4energy.processor.service.validation;

import com.com4energy.persistence.filerecord.enums.FailureReason;
import com.com4energy.processor.service.FileRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Guard del chain de subida: rechaza un archivo cuyo CONTENIDO ya existe en BD
 * ({@code DUPLICATED_CONTENT}, hash SHA-256) aunque llegue con un nombre distinto. Segunda barrera
 * para el reenvío idéntico que esquiva el guard de nombre.
 */
class DuplicatedContentByHashValidatorTest {

    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private final DuplicatedContentByHashValidator validator =
            new DuplicatedContentByHashValidator(fileRecordService);

    @Test
    void rejectsWhenContentHashAlreadyExists() {
        when(fileRecordService.existsByHash(anyString())).thenReturn(true);

        assertEquals(
                Optional.of(FailureReason.DUPLICATED_CONTENT),
                validator.validate(contextFor("nombre-nuevo.xml", "contenido-existente"))
        );
    }

    @Test
    void acceptsWhenContentIsNew() {
        when(fileRecordService.existsByHash(anyString())).thenReturn(false);

        assertTrue(validator.validate(contextFor("archivo.xml", "contenido-nuevo")).isEmpty());
    }

    private ValidationContext contextFor(String filename, String content) {
        return ValidationContext.from(
                new MockMultipartFile("file", filename, "application/octet-stream", content.getBytes()));
    }
}
