package com.com4energy.processor.service.validation;

import com.com4energy.processor.config.properties.FileUploadProperties;
import com.com4energy.persistence.filerecord.enums.FailureReason;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Step 5 – rejects files whose extension is not allowed.
 *
 * <p>An extension is accepted when it is either:
 * <ul>
 *   <li>a literal extension from {@link FileUploadProperties#normalizedAllowedExtensions()}
 *       (e.g. {@code xml}), or</li>
 *   <li>a single-digit measure-version extension {@code 0–9} (e.g. {@code P1D_..._20260502.3}).</li>
 * </ul>
 *
 * <p>Multi-digit version extensions (e.g. {@code .10}) are intentionally rejected:
 * the naming convention caps version/iteration at a single digit. See
 * {@link com.com4energy.processor.util.FileNameVersionParserUtil}.
 *
 * <p>{@link FilenameValidator} runs before this step, guaranteeing that the
 * originalFilename is non-null and free of traversal characters.
 */
@Component
@Order(120)
@RequiredArgsConstructor
public class FileExtensionValidator implements FileValidator {

    /** Single-digit measure-version extension (revision/iteration), 0–9. */
    private static final Pattern VERSION_EXTENSION = Pattern.compile("\\d");

    private final FileUploadProperties props;

    @Override
    public Optional<FailureReason> validate(ValidationContext context) {
        if (context.getOriginalFilename() == null) {
            return Optional.empty();
        }

        String ext = context.getExtension();

        if (ext == null || !isAllowed(ext)) {
            return Optional.of(FailureReason.INVALID_EXTENSION);
        }
        return Optional.empty();
    }

    private boolean isAllowed(String ext) {
        return props.normalizedAllowedExtensions().contains(ext)
                || VERSION_EXTENSION.matcher(ext).matches();
    }

    @Override
    public ValidationMode mode() {
        return ValidationMode.COLLECT_ALL;
    }

}
