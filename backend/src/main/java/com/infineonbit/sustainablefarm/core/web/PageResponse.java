package com.infineonbit.sustainablefarm.core.web;

import java.util.List;

/**
 * One page of a paginated list, the shared JSON shape of every module:
 * {@code content}, {@code page} (zero based), {@code size},
 * {@code totalElements} and {@code totalPages}.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
