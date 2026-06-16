package com.com4energy.processor.service.validation;

import com.com4energy.persistence.filerecord.enums.FailureReason;
import com.com4energy.processor.service.measure.MeasureRevisionGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Step 310 – rechazo temprano de archivos de medida cuya (revision, iteration) no es estrictamente
 * más nueva que la ya registrada para su familia (source_family_key).
 *
 * <p>Complementa a los validadores de duplicado: estos atrapan re-subidas exactas (mismo nombre/hash);
 * este atrapa una revisión más vieja con OTRO nombre (p.ej. {@code .2} tras un {@code .3}). Solo aplica
 * a archivos de medida versionados (los no versionados, como xml, pasan). Hace una consulta indexada en
 * upload (no en el bulk), por eso va al final junto a las demás validaciones que tocan BD.
 */
@Component
@Order(310)
@RequiredArgsConstructor
public class SupersededRevisionValidator implements FileValidator {

    private final MeasureRevisionGuard measureRevisionGuard;

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        String originalFilename = context.getOriginalFilename();
        if (originalFilename == null) {
            return Optional.empty();
        }
        if (measureRevisionGuard.isSupersededAtUpload(originalFilename)) {
            return Optional.of(FailureReason.SUPERSEDED_REVISION);
        }
        return Optional.empty();
    }

}
