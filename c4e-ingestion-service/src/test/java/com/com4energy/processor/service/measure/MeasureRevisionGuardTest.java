package com.com4energy.processor.service.measure;

import com.com4energy.persistence.filerecord.MeasureFileVersion;
import com.com4energy.processor.repository.FileRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeasureRevisionGuardTest {

    private final FileRecordRepository fileRecordRepository = mock(FileRecordRepository.class);
    private final MeasureRevisionGuard guard = new MeasureRevisionGuard(fileRecordRepository);

    @Test
    void notSupersededWhenNoPreviousVersion() {
        when(fileRecordRepository.findFamilyVersions(any(), any(), any(Pageable.class))).thenReturn(List.of());

        assertFalse(guard.isSupersededByApplied(new MeasureFileVersion("FAM", 0, 0)));
    }

    @Test
    void supersededWhenSameRevisionAndIteration() {
        when(fileRecordRepository.findFamilyVersions(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(version(3, 0)));

        assertTrue(guard.isSupersededByApplied(new MeasureFileVersion("FAM", 3, 0)));
    }

    @Test
    void notSupersededWhenHigherRevision() {
        when(fileRecordRepository.findFamilyVersions(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(version(3, 0)));

        assertFalse(guard.isSupersededByApplied(new MeasureFileVersion("FAM", 4, 0)));
    }

    @Test
    void notSupersededWhenSameRevisionHigherIteration() {
        when(fileRecordRepository.findFamilyVersions(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(version(3, 0)));

        assertFalse(guard.isSupersededByApplied(new MeasureFileVersion("FAM", 3, 1)));
    }

    @Test
    void supersededWhenLowerIterationOfSameRevision() {
        when(fileRecordRepository.findFamilyVersions(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(version(3, 2)));

        assertTrue(guard.isSupersededByApplied(new MeasureFileVersion("FAM", 3, 1)));
    }

    @Test
    void uploadGuardParsesFilenameAndDetectsSuperseded() {
        when(fileRecordRepository.findFamilyVersions(eq("P1D_0031_0894_20260502"), any(), any(Pageable.class)))
                .thenReturn(List.of(version(3, 0)));

        assertTrue(guard.isSupersededAtUpload("P1D_0031_0894_20260502.3"));
    }

    @Test
    void uploadGuardIgnoresNonMeasureFiles() {
        assertFalse(guard.isSupersededAtUpload("factura_20260502.xml"));
        verify(fileRecordRepository, never()).findFamilyVersions(any(), any(), any(Pageable.class));
    }

    private FileRecordRepository.AppliedVersionView version(Integer revision, Integer iteration) {
        return new FileRecordRepository.AppliedVersionView() {
            @Override
            public Integer getRevision() {
                return revision;
            }

            @Override
            public Integer getProcessingIteration() {
                return iteration;
            }
        };
    }
}
