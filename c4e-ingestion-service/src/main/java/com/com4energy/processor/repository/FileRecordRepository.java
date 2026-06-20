package com.com4energy.processor.repository;

import com.com4energy.persistence.filerecord.BaseFileRecordRepository;
import com.com4energy.persistence.filerecord.FileRecord;
import com.com4energy.persistence.filerecord.enums.FileStatus;
import com.com4energy.persistence.filerecord.enums.FileType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRecordRepository extends BaseFileRecordRepository {

    /**
     * Versiones (revision, iteration) registradas para una familia de archivos de medida, en los
     * estados dados, ordenadas de mayor a menor. Para el guard de precedencia de revisión: el
     * primer resultado es la versión más alta ya aplicada/viva. Usa idx_file_records_family_version.
     */
    @Query("""
            select fr.measureVersion.revision as revision,
                   fr.measureVersion.processingIteration as processingIteration
            from FileRecord fr
            where fr.measureVersion.sourceFamilyKey = :family
              and fr.status in :statuses
            order by fr.measureVersion.revision desc, fr.measureVersion.processingIteration desc
            """)
    List<AppliedVersionView> findFamilyVersions(
            @Param("family") String family,
            @Param("statuses") Collection<FileStatus> statuses,
            Pageable pageable
    );

    interface AppliedVersionView {
        Integer getRevision();
        Integer getProcessingIteration();
    }

    boolean existsByOriginalFilenameAndFinalPath(String originalFilename, String finalPath);

    boolean existsByHash(String hash);

    boolean existsByOriginalFilename(String originalFilename);

    /**
     * True si ya existe un archivo con la MISMA versión lógica (familia, revisión, iteración),
     * sin importar el nombre crudo ni el estado. Cierra el caso {@code .4} ≡ {@code .4.0}: mismo
     * (familia, revisión, iteración) aunque el nombre difiera. No considera versiones mayores ni
     * menores — solo la tupla exacta.
     */
    @Query("""
            select count(fr) > 0
            from FileRecord fr
            where fr.measureVersion.sourceFamilyKey = :family
              and fr.measureVersion.revision = :revision
              and fr.measureVersion.processingIteration = :iteration
            """)
    boolean existsByMeasureVersion(
            @Param("family") String family,
            @Param("revision") Integer revision,
            @Param("iteration") Integer iteration
    );

    Optional<FileRecord> findFirstByOriginalFilename(String originalFilename);

    @Query("SELECT f.originalFilename FROM FileRecord f WHERE f.originalFilename LIKE :pattern")
    List<String> findAllOriginalFilenamesLike(@Param("pattern") String pattern);

    @Query("""
            SELECT COUNT(f) > 0 FROM FileRecord f
            WHERE f.originalFilename LIKE :familyPattern
              AND f.id <> :excludeId
              AND f.locked = true
              AND f.status = :processingStatus
            """)
    boolean existsFamilyFileBeingProcessed(
            @Param("familyPattern") String familyPattern,
            @Param("excludeId") Long excludeId,
            @Param("processingStatus") FileStatus processingStatus
    );

    Optional<FileRecord> findByOriginalFilenameAndFinalPath(String originalFilename, String finalPath);

    Optional<FileRecord> findByOriginalFilenameAndFinalPathOrHash(String originalFilename, String finalPath, String fileHash);

    Optional<FileRecord> findByHash(String hash);

    List<FileRecord> findByStatus(FileStatus status);

    List<FileRecord> findByStatusIn(List<FileStatus> statuses);

    List<FileRecord> findByLockedTrueAndLockedAtBefore(LocalDateTime threshold);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f
            from FileRecord f
            where f.status in :statuses
              and (f.locked = false or f.locked is null)
              and f.type in :types
            order by f.uploadedAt asc, f.id asc
            """)
    List<FileRecord> findCandidatesForProcessing(
            @Param("statuses") List<FileStatus> statuses,
            @Param("types") List<FileType> types,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f
            from FileRecord f
            where f.id = :id
            """)
    Optional<FileRecord> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select f
            from FileRecord f
            where f.id = :id
              and f.locked = true
              and f.lockedBy = :lockedBy
            """)
    Optional<FileRecord> findOwnedByIdForUpdate(@Param("id") Long id, @Param("lockedBy") String lockedBy);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update FileRecord f
            set f.locked = false,
                f.lockedBy = null,
                f.lockedAt = null
            where f.id = :id
              and f.locked = true
              and f.lockedBy = :lockedBy
            """)
    int releaseLockIfOwnedBy(@Param("id") Long id, @Param("lockedBy") String lockedBy);

}
