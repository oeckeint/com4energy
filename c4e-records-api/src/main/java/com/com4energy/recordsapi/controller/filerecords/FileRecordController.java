package com.com4energy.recordsapi.controller.filerecords;

import com.com4energy.recordsapi.controller.common.ResponseHelper;
import com.com4energy.recordsapi.controller.common.dto.PageResponse;
import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordListItemResponse;
import com.com4energy.recordsapi.controller.medidas.DateRangeHelper;
import com.com4energy.recordsapi.service.FileRecordQueryService;
import com.com4energy.recordsapi.service.dto.FileRecordQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping(FileRecordConstants.BASE_PATH)
@RequiredArgsConstructor
public class FileRecordController {

    private final FileRecordQueryService service;

    @GetMapping
    public ResponseEntity<PageResponse<FileRecordListItemResponse>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "origin", required = false) String origin,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "fileType", required = false) String fileType,
            @RequestParam(name = "filename", required = false) String filename
    ) {
        LocalDateTime start = DateRangeHelper.parseDate(startDate, false);
        LocalDateTime end = DateRangeHelper.parseDate(endDate, true);

        FileRecordQueryCriteria criteria = new FileRecordQueryCriteria(
                page,
                size,
                sortBy,
                sortDir,
                start,
                end,
                origin,
                status,
                fileType,
                filename
        );

        return ResponseHelper.ok(service.list(criteria));
    }
}


