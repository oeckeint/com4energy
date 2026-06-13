package com.com4energy.persistence.filerecord;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadatos de versionado de un archivo de medidas.
 * Solo tiene valor cuando el archivo sigue la convención de nombre:
 *   archivo.{revision} o archivo.{revision}.{iteration}
 * Para archivos XML u otros formatos este objeto es null en FileRecord.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeasureFileVersion {

    @Column(name = "source_family_key")
    private String sourceFamilyKey;

    @Column(name = "revision")
    private Integer revision;

    @Column(name = "processing_iteration")
    private Integer processingIteration;

}
