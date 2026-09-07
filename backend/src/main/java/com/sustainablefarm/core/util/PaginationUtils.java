package com.sustainablefarm.core.util;

import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.PageResponse;

import java.util.Collections;
import java.util.List;

/**
 * Utility for in-memory pagination of collection results.
 */
public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static <T> PageResponse<T> paginate(List<T> items, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        int totalElements = items.size();
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        List<T> content = fromIndex >= toIndex
                ? Collections.emptyList()
                : items.subList(fromIndex, toIndex);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);

        return PageResponse.<T>builder()
                .content(content)
                .page(safePage)
                .size(safeSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
