package com.com4energy.processor.service.measure;

import com.com4energy.persistence.filerecord.MeasureFileVersion;
import com.com4energy.persistence.filerecord.enums.FileStatus;
import com.com4energy.processor.repository.FileRecordRepository;
import com.com4energy.processor.util.FileNameVersionParserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Precedencia de revisión de archivos de medida: gana la (revision, iteration) más alta de una
 * familia (source_family_key). Un archivo que no es estrictamente más nuevo que lo ya registrado
 * queda superseded.
 *
 * <p>Se usa en dos puntos:
 * <ul>
 *   <li>Validación de upload (rechazo temprano) — contra estados "vivos" de la familia.</li>
 *   <li>Antes del upsert (autoritativo) — contra lo ya aplicado (SUCCEEDED).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class MeasureRevisionGuard {

    /** Estados que cuentan como "ya en el sistema" para el rechazo temprano en upload. */
    private static final List<FileStatus> LIVE_STATUSES = List.of(
            FileStatus.SUCCEEDED, FileStatus.PENDING, FileStatus.PROCESSING,
            FileStatus.RETRY, FileStatus.UPLOADED, FileStatus.VALID, FileStatus.NEW
    );

    /** Estados que cuentan como "ya aplicado" para el guard autoritativo antes del upsert. */
    private static final List<FileStatus> APPLIED_STATUSES = List.of(FileStatus.SUCCEEDED);

    private final FileRecordRepository fileRecordRepository;

    /** Rechazo temprano en upload: parsea la versión del nombre y compara contra los estados vivos. */
    public boolean isSupersededAtUpload(String originalFilename) {
        FileNameVersionParserUtil.Result version = FileNameVersionParserUtil.parse(originalFilename);
        if (version.sourceFamilyKey() == null || version.revision() == null) {
            return false; // no es un archivo de medida versionado (p.ej. xml)
        }
        return isSuperseded(version.sourceFamilyKey(), version.revision(),
                iterationOrZero(version.processingIteration()), LIVE_STATUSES);
    }

    /**
     * ¿Existe ya un archivo aplicado (SUCCEEDED) de esta familia? Si NO, es la primera carga de
     * la familia: no hay medidas previas que revisar, así que el upsert puede omitir la pre-carga
     * y tratar todo como INSERT. (Asume que las medidas no se solapan entre familias por
     * (id_cliente, fecha); si se solaparan, el INSERT chocaría con uk_business y caería a
     * cuarentena por binary-split — fallo visible, no corrupción silenciosa.)
     */
    public boolean hasAppliedSibling(MeasureFileVersion version) {
        if (version == null || version.getSourceFamilyKey() == null) {
            return false;
        }
        return !fileRecordRepository
                .findFamilyVersions(version.getSourceFamilyKey(), APPLIED_STATUSES, PageRequest.of(0, 1))
                .isEmpty();
    }

    /** Guard autoritativo antes del upsert: compara contra lo ya aplicado (SUCCEEDED). */
    public boolean isSupersededByApplied(MeasureFileVersion version) {
        if (version == null || version.getSourceFamilyKey() == null || version.getRevision() == null) {
            return false;
        }
        return isSuperseded(version.getSourceFamilyKey(), version.getRevision(),
                iterationOrZero(version.getProcessingIteration()), APPLIED_STATUSES);
    }

    private boolean isSuperseded(String family, int revision, int iteration, List<FileStatus> statuses) {
        List<FileRecordRepository.AppliedVersionView> top =
                fileRecordRepository.findFamilyVersions(family, statuses, PageRequest.of(0, 1));
        if (top.isEmpty()) {
            return false; // no hay versión previa -> procede
        }
        FileRecordRepository.AppliedVersionView existing = top.get(0);
        int existingRevision = existing.getRevision() == null ? Integer.MIN_VALUE : existing.getRevision();
        int existingIteration = iterationOrZero(existing.getProcessingIteration());

        boolean strictlyNewer = revision > existingRevision
                || (revision == existingRevision && iteration > existingIteration);
        return !strictlyNewer;
    }

    private int iterationOrZero(Integer iteration) {
        return iteration == null ? 0 : iteration;
    }
}
