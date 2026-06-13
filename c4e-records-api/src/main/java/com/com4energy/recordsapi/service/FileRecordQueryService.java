package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.controller.common.dto.PageResponse;
import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordListItemResponse;
import com.com4energy.recordsapi.repository.FileRecordQueryRepository;
import com.com4energy.recordsapi.service.dto.FileRecordQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileRecordQueryService {

    private final FileRecordQueryRepository repository;

    public PageResponse<FileRecordListItemResponse> list(FileRecordQueryCriteria criteria) {
        int safePage = Math.max(criteria.page(), 0);
        int safeSize = Math.min(Math.max(criteria.size(), 1), 200);

        FileRecordQueryCriteria safeCriteria = new FileRecordQueryCriteria(
                safePage,
                safeSize,
                criteria.sortBy(),
                criteria.sortDir(),
                criteria.startDate(),
                criteria.endDate(),
                criteria.origin(),
                criteria.status(),
                criteria.fileType(),
                criteria.filename()
        );

        long total = repository.count(safeCriteria);
        List<FileRecordListItemResponse> data = repository.fetchPage(safeCriteria);

        int totalPages = (int) Math.ceil((double) total / safeSize);

        return new PageResponse<>(
                data,
                safePage,
                safeSize,
                total,
                totalPages,
                safePage == 0,
                safePage >= Math.max(totalPages - 1, 0)
        );
    }
}


