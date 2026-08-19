package com.sustainablefarm.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Paginated collection response wrapper.
 */
@Getter
@Builder
public class PageResponse<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
