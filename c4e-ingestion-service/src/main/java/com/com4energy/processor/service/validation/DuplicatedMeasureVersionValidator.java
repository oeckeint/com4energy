package com.com4energy.processor.service.validation;

import com.com4energy.persistence.filerecord.enums.FailureReason;
import com.com4energy.processor.service.FileRecordService;
import com.com4energy.processor.util.FileNameVersionParserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Step 8 – rechaza un archivo de medidas cuya <em>versión lógica</em>
 * {@code (familia, revisión, iteración)} ya existe en la base de datos, aunque el nombre crudo
 * difiera.
 *
 * <p>Caso que cierra: {@code P1D_...._20260502.4} y {@code P1D_...._20260502.4.0} son nombres
 * distintos (el {@link DuplicatedOriginalFilenameValidator} no los empareja) y pueden tener
 * contenido distinto (el {@link DuplicatedContentByHashValidator} tampoco los empareja), pero
 * ambos representan la MISMA versión {@code (familia=P1D_...._20260502, revisión=4, iteración=0)}.
 * Sin este guard, el segundo se procesaría como un last-write-wins silencioso sobre el primero.
 *
 * <p>Corre después del guard de nombre exacto (210) y antes del de contenido (300): si el nombre
 * coincide exacto se reporta el motivo más específico ({@code DUPLICATED_ORIGINAL_FILENAME}).
 *
 * <p>No aplica a archivos que no siguen la convención de versión (XML, otros): en ese caso el
 * parseo devuelve familia {@code null} y el validator no toca la BD.
 *
 * <p>No rechaza versiones mayores ni menores — solo la tupla exacta; bumpear la iteración
 * ({@code .4.1}) sigue siendo una versión nueva válida.
 */
@Component
@Order(220)
@RequiredArgsConstructor
public class DuplicatedMeasureVersionValidator implements FileValidator {

    private final FileRecordService fileRecordService;

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        FileNameVersionParserUtil.Result version =
                FileNameVersionParserUtil.parse(context.getOriginalFilename());

        if (version.sourceFamilyKey() == null) {
            return Optional.empty();
        }

        boolean alreadyExists = fileRecordService.existsByMeasureVersion(
                version.sourceFamilyKey(), version.revision(), version.processingIteration());

        return alreadyExists ? Optional.of(FailureReason.DUPLICATED_VERSION) : Optional.empty();
    }

}
