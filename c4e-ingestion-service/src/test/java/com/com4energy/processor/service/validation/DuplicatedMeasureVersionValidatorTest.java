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
 * Guard de versión lógica: empareja {@code .4} con {@code .4.0} (misma familia/revisión/iteración)
 * aunque el nombre crudo difiera, cerrando el last-write-wins silencioso. No rechaza versiones
 * mayores/menores ni archivos fuera de la convención de versión.
 */
class DuplicatedMeasureVersionValidatorTest {

    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private final DuplicatedMeasureVersionValidator validator =
            new DuplicatedMeasureVersionValidator(fileRecordService);

    @Test
    void rejectsWhenSameLogicalVersionAlreadyExistsUnderDifferentName() {
        // ".4" ya está en BD como (familia, 4, 0); llega ".4.0" -> misma versión lógica.
        when(fileRecordService.existsByMeasureVersion("P1D_0031_0894_20260502", 4, 0)).thenReturn(true);

        assertEquals(
                Optional.of(FailureReason.DUPLICATED_VERSION),
                validator.validate(contextFor("P1D_0031_0894_20260502.4.0"))
        );
    }

    @Test
    void acceptsWhenLogicalVersionIsNew() {
        when(fileRecordService.existsByMeasureVersion("P1D_0031_0894_20260502", 5, 0)).thenReturn(false);

        assertTrue(validator.validate(contextFor("P1D_0031_0894_20260502.5")).isEmpty());
    }

    @Test
    void doesNotRejectADifferentIterationOfTheSameRevision() {
        // ".4.0" existe; ".4.1" es una iteración nueva, NO un duplicado.
        when(fileRecordService.existsByMeasureVersion("P1D_0031_0894_20260502", 4, 1)).thenReturn(false);

        assertTrue(validator.validate(contextFor("P1D_0031_0894_20260502.4.1")).isEmpty());
    }

    @Test
    void skipsAndDoesNotHitDbWhenFilenameIsNotAVersionedMeasureFile() {
        assertTrue(validator.validate(contextFor("clientes.xml")).isEmpty());
        verifyNoInteractions(fileRecordService);
    }

    private ValidationContext contextFor(String filename) {
        return ValidationContext.from(
                new MockMultipartFile("file", filename, "application/octet-stream", "x".getBytes()));
    }
}
