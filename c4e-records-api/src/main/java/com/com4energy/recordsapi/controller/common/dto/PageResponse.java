package com.com4energy.recordsapi.controller.common.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResponse<T> {

    private final List<T> data;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    public PageResponse(
            List<T> data,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {

        this.data = data;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

}
