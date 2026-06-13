package com.com4energy.processor.util;

import org.apache.commons.io.FilenameUtils;

/**
 * Extrae source_family_key, revision y processing_iteration del nombre de un archivo de medidas.
 *
 * Convención de nombres:
 *   archivo.{revision}                   →  P1D_0031_0894_20260502.3
 *   archivo.{revision}.{iteration}       →  P1D_0031_0894_20260502.3.4
 *
 * Las extensiones de versión son siempre un único dígito (0-9).
 * Si el nombre no sigue la convención, los tres campos quedan en null / 0.
 */
public final class FileNameVersionParserUtil {

    private FileNameVersionParserUtil() {
    }

    public record Result(
            String sourceFamilyKey,
            Integer revision,
            Integer processingIteration
    ) {
        static Result empty() {
            return new Result(null, null, null);
        }
    }

    public static Result parse(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return Result.empty();
        }

        String lastExt = FilenameUtils.getExtension(originalFilename);
        if (!isSingleDigit(lastExt)) {
            return Result.empty();
        }

        String withoutLastExt = FilenameUtils.getBaseName(originalFilename);
        String midExt = FilenameUtils.getExtension(withoutLastExt);

        if (isSingleDigit(midExt)) {
            // Doble extensión: archivo.{revision}.{iteration}
            String familyKey = FilenameUtils.getBaseName(withoutLastExt);
            return new Result(familyKey, Integer.parseInt(midExt), Integer.parseInt(lastExt));
        }

        // Extensión simple: archivo.{revision}
        return new Result(withoutLastExt, Integer.parseInt(lastExt), 0);
    }

    private static boolean isSingleDigit(String s) {
        return s != null && s.length() == 1 && Character.isDigit(s.charAt(0));
    }
}
