package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.controller.common.dto.PageResponse;
import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordEventListItemResponse;
import com.com4energy.recordsapi.repository.FileRecordEventQueryRepository;
import com.com4energy.recordsapi.service.dto.FileRecordEventQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileRecordEventQueryService {

    private final FileRecordEventQueryRepository repository;

    public PageResponse<FileRecordEventListItemResponse> list(FileRecordEventQueryCriteria criteria) {
        int safePage = Math.max(criteria.page(), 0);
        int safeSize = Math.min(Math.max(criteria.size(), 1), 200);

        FileRecordEventQueryCriteria safeCriteria = new FileRecordEventQueryCriteria(
                safePage,
                safeSize,
                criteria.sortBy(),
                criteria.sortDir(),
                criteria.startDate(),
                criteria.endDate(),
                criteria.eventType(),
                criteria.status(),
                criteria.origin(),
                criteria.fileType(),
                criteria.filename()
        );

        long total = repository.count(safeCriteria);
        List<FileRecordEventListItemResponse> data = repository.fetchPage(safeCriteria);

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

